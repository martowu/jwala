package com.siemens.cto.aem.service.state.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.annotation.Splitter;
import org.springframework.transaction.annotation.Transactional;

import com.siemens.cto.aem.domain.model.audit.AuditEvent;
import com.siemens.cto.aem.domain.model.event.Event;
import com.siemens.cto.aem.domain.model.group.CurrentGroupState;
import com.siemens.cto.aem.domain.model.group.Group;
import com.siemens.cto.aem.domain.model.group.GroupState;
import com.siemens.cto.aem.domain.model.group.LiteGroup;
import com.siemens.cto.aem.domain.model.group.command.ControlGroupCommand;
import com.siemens.cto.aem.domain.model.group.command.SetGroupStateCommand;
import com.siemens.cto.aem.domain.model.id.Identifier;
import com.siemens.cto.aem.domain.model.jvm.Jvm;
import com.siemens.cto.aem.domain.model.jvm.JvmState;
import com.siemens.cto.aem.domain.model.state.CurrentState;
import com.siemens.cto.aem.domain.model.state.StateType;
import com.siemens.cto.aem.domain.model.temporary.User;
import com.siemens.cto.aem.domain.model.webserver.WebServer;
import com.siemens.cto.aem.domain.model.webserver.WebServerReachableState;
import com.siemens.cto.aem.persistence.dao.webserver.WebServerDao;
import com.siemens.cto.aem.persistence.service.group.GroupPersistenceService;
import com.siemens.cto.aem.persistence.service.jvm.JvmPersistenceService;
import com.siemens.cto.aem.persistence.service.state.StatePersistenceService;
import com.siemens.cto.aem.service.group.GroupStateMachine;
import com.siemens.cto.aem.service.group.impl.LockableGroupStateMachine;
import com.siemens.cto.aem.service.group.impl.LockableGroupStateMachine.Initializer;
import com.siemens.cto.aem.service.group.impl.LockableGroupStateMachine.Lease;
import com.siemens.cto.aem.service.group.impl.LockableGroupStateMachine.ReadWriteLease;
import com.siemens.cto.aem.service.state.GroupStateService;
import com.siemens.cto.aem.service.state.StateNotificationGateway;
import com.siemens.cto.aem.service.state.StateNotificationService;
import com.siemens.cto.aem.service.state.StateService;


/**
 * Invoked in response to incoming state changes - jvm or web server
 */
public class GroupStateServiceImpl extends StateServiceImpl<Group, GroupState> implements StateService<Group, GroupState>, GroupStateService.API, ApplicationContextAware {

    public GroupStateServiceImpl(StatePersistenceService<Group, GroupState> thePersistenceService,
            StateNotificationService theNotificationService, StateType theStateType,
            StateNotificationGateway theStateNotificationGateway) {
        super(thePersistenceService, theNotificationService, theStateType, theStateNotificationGateway);

        systemUser = User.getSystemUser();
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GroupStateServiceImpl.class);

    @Autowired
    private GroupPersistenceService groupPersistenceService;

    @Autowired
    private JvmPersistenceService jvmPersistenceService;

    @Autowired
    private WebServerDao webServerDao;

    private ConcurrentHashMap<Identifier<Group>, LockableGroupStateMachine> allGSMs = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    private User systemUser;

    @Transactional(readOnly=true)
    @Override
    @Splitter
    public List<SetGroupStateCommand> stateUpdateJvm(CurrentState<Jvm, JvmState> cjs) throws InterruptedException {

        LOGGER.debug("Recalculating group state due to jvm update: " + cjs.toString());

        // lookup children
        Identifier<Jvm> jvmId = cjs.getId();
        Jvm jvm = jvmPersistenceService.getJvm(jvmId);

        if(jvm == null) {
            return Collections.<SetGroupStateCommand>emptyList();
        }

        Set<LiteGroup> groups = jvm.getGroups();

        if(groups == null || groups.isEmpty()) {
            return Collections.<SetGroupStateCommand>emptyList();
        }

        List<SetGroupStateCommand> result = null;
        result = new ArrayList<>(groups.size());

        try {
            for(LiteGroup group : groups) {

                final Identifier<Group> groupId = group.getId();

                Group fullGroup = groupPersistenceService.getGroup(groupId);
                CurrentGroupState priorState = fullGroup.getCurrentState();

                LockableGroupStateMachine lockableGsm = this.getLockableGsm(groupId);
                ReadWriteLease gsm = lockableGsm.tryPersistentLock(new Initializer() {
                    @Override
                    public GroupStateMachine initializeGroupStateMachine() {
                        GroupStateMachine newGsm = applicationContext.getBean("groupStateMachine", GroupStateMachine.class);
                        Group group = groupPersistenceService.getGroup(groupId);
                        newGsm.synchronizedInitializeGroup(group, systemUser);
                        return newGsm;
                    }
                }, 1, TimeUnit.SECONDS);

                if(gsm == null) {
                    // could not lock
                    LOGGER.warn("Skipping group due to lock {}", group);
                    continue;
                }
                                
                gsm.refreshState();
                
                if(priorState != null &&
                    priorState.getState() != gsm.getCurrentState() ) {
                        lockableGsm.setDirty(true);                    
                }

                SetGroupStateCommand sgsc= new SetGroupStateCommand(gsm.getCurrentStateDetail());

                result.add(sgsc);
            }
        } catch(final RuntimeException re) {
            LOGGER.warn("GSS Unlocking affected groups due to exception.", re);
            for(SetGroupStateCommand sgsc : result) {
                this.groupStateUnlock(sgsc);
            }
            result.clear();
            throw re;
        }

        return result;
    }

    @Transactional(readOnly=true)
    @Override
    @Splitter
    public List<SetGroupStateCommand>  stateUpdateWebServer(CurrentState<WebServer, WebServerReachableState> wsState) throws InterruptedException {
        LOGGER.debug("GSS Recalc group state due to web server update: " + wsState.toString());

        // lookup children
        Identifier<WebServer> wsId = wsState.getId();
        WebServer ws = webServerDao.getWebServer(wsId);

        if(ws == null) {
            return Collections.<SetGroupStateCommand>emptyList();
        }

        Collection<Group> groups = ws.getGroups();

        if(groups == null || groups.isEmpty()) {
            return Collections.<SetGroupStateCommand>emptyList();
        }

        List<SetGroupStateCommand> result = new ArrayList<>(groups.size());

        try {
            for(Group group : groups) {

                final Identifier<Group> groupId = group.getId();
                CurrentState<Group, GroupState> priorState = group.getCurrentState();

                LockableGroupStateMachine lockableGsm = this.getLockableGsm(groupId);
                ReadWriteLease gsm = lockableGsm.tryPersistentLock(new Initializer() {
                    @Override
                    public GroupStateMachine initializeGroupStateMachine() {
                        GroupStateMachine newGsm = applicationContext.getBean("groupStateMachine", GroupStateMachine.class);
                        Group group = groupPersistenceService.getGroup(groupId);
                        newGsm.synchronizedInitializeGroup(group, systemUser);
                        return newGsm;
                    }
                }, 1, TimeUnit.SECONDS);

                if(gsm == null) {
                    // could not lock
                    LOGGER.warn("GSS Skipping group due to lock {}", group);
                    continue;
                }

                gsm.refreshState();
                
                if(priorState != null &&
                        priorState.getState() != gsm.getCurrentState() ) {
                            lockableGsm.setDirty(true);                    
                }
                
                SetGroupStateCommand sgsc= new SetGroupStateCommand(gsm.getCurrentStateDetail());

                result.add(sgsc);
            }
        } catch(final RuntimeException re) {
            LOGGER.warn("GSS Unlocking affected groups due to exception.", re);
            for(SetGroupStateCommand sgsc : result) {
                this.groupStateUnlock(sgsc);
            }
            result.clear();
            throw re;
        }
        return result;
    }

    /**
     * @param groupId group to get a state machine for.
     * @return the state machine
     */
    private LockableGroupStateMachine getLockableGsm(final Identifier<Group> groupId) {
        LockableGroupStateMachine tempGsm;
        LockableGroupStateMachine actualGsm;
        actualGsm = allGSMs.putIfAbsent(groupId, tempGsm = new LockableGroupStateMachine());

        if(actualGsm == null) {
            actualGsm = tempGsm;
        }

        return actualGsm;
    }

    /**
     * @param groupId group to get a state machine for.
     * @return the state machine
     */
    private ReadWriteLease leaseWritableGsm(final Identifier<Group> groupId, final User user) {
        LockableGroupStateMachine gsm = getLockableGsm(groupId);

        ReadWriteLease lockedGsmLease = gsm.lockForWriteWithResources(new Initializer() {
            @Override
            public GroupStateMachine initializeGroupStateMachine() {
                GroupStateMachine newGsm = applicationContext.getBean("groupStateMachine", GroupStateMachine.class);
                Group group = groupPersistenceService.getGroup(groupId);
                newGsm.synchronizedInitializeGroup(group, user);
                return newGsm;
            }
        });

        return lockedGsmLease;
    }
    /**
     * @param groupId group to get a state machine for.
     * @return the state machine
     */
    private Lease getGsmWithResources(final Identifier<Group> groupId, final User user) {
        LockableGroupStateMachine gsm = getLockableGsm(groupId);

        Lease lockedGsmLease = gsm.lockForReadWithResources(new Initializer() {
            @Override
            public GroupStateMachine initializeGroupStateMachine() {
                GroupStateMachine newGsm = applicationContext.getBean("groupStateMachine", GroupStateMachine.class);
                Group group = groupPersistenceService.getGroup(groupId);
                newGsm.synchronizedInitializeGroup(group, user);
                return newGsm;
            }
        });

        return lockedGsmLease;
    }

    private RuntimeException convert(Exception e) {
        return new RuntimeException(e);
    }

    @Override
    public CurrentGroupState signalReset(Identifier<Group> groupId, User user) {
        try(Lease lease = leaseWritableGsm(groupId, user)) {
            return lease.signalReset(user);
        } catch (Exception e) {
            throw convert(e);
        }
    }

    @Override
    public CurrentGroupState signalStopRequested(Identifier<Group> groupId, User user) {
        try(Lease lease = leaseWritableGsm(groupId, user)) {
            return lease.signalStopRequested(user);
        } catch (Exception e) {
            throw convert(e);
        }
    }

    @Override
    public CurrentGroupState signalStartRequested(Identifier<Group> groupId, User user) {
        try(Lease lease = leaseWritableGsm(groupId, user)) {
            return lease.signalStartRequested(user);
        } catch (Exception e) {
            throw convert(e);
        }
    }

    @Override
    public boolean canStart(Identifier<Group> groupId, User user) {
        try(Lease lease = getGsmWithResources(groupId, user).readOnly()) {
            return lease.canStart();
        } catch (Exception e) {
            throw convert(e);
        }
    }

    @Override
    public boolean canStop(Identifier<Group> groupId, User user) {
        try(Lease lease = getGsmWithResources(groupId, user).readOnly()) {
            return lease.canStop();
        } catch (Exception e) {
            throw convert(e);
        }
    }

    @Override
    protected CurrentState<Group, GroupState> createUnknown(Identifier<Group> anId) {
        return new CurrentGroupState(anId, GroupState.UNKNOWN, DateTime.now());
    }

    @Override
    public CurrentGroupState signal(ControlGroupCommand aCommand, User aUser) {
        switch(aCommand.getControlOperation()) {
        case START:
            return signalStartRequested(aCommand.getGroupId(), aUser);
        case STOP:
            return signalStopRequested(aCommand.getGroupId(), aUser);
        default:
            return null;
        }
    }

    @Override
    @Transactional
    public SetGroupStateCommand groupStatePersist(SetGroupStateCommand sgsc) {
        // If an empty list is returned by the splitter, it will be treated as single null item, so check
        try {
            if(sgsc != null && sgsc.getNewState() != null) {
                LOGGER.trace("GSS Persist: {}", sgsc.getNewState());
                groupPersistenceService.updateGroupStatus(Event.create(sgsc, AuditEvent.now(systemUser)));
            }
        } catch(final RuntimeException re) {
            LOGGER.warn("GSS Unlocking group due to database exception.", re);
            groupStateUnlock(sgsc);
            throw re;
        }
        return sgsc;
    }

    @Override
    public SetGroupStateCommand groupStateNotify(SetGroupStateCommand sgsc) {
        // If an empty list is returned by the splitter, it will be treated as single null item, so check
        if(sgsc != null && sgsc.getNewState() != null) {
            if(getLockableGsm(sgsc.getNewState().getId()).isDirty()) {
                LOGGER.trace("GSS Notify: {}", sgsc.getNewState());
                getNotificationService().notifyStateUpdated(sgsc.getNewState());
            } else { 
                LOGGER.trace("GSS Discard Notify (Same State): {}", sgsc.getNewState());
            }
            getLockableGsm(sgsc.getNewState().getId()).setDirty(false);
        }
        return sgsc;
    }

    @Override
    public SetGroupStateCommand groupStateUnlock(SetGroupStateCommand sgsc) {
     // If an empty list is returned by the splitter, it will be treated as single null item, so check
        if(sgsc != null && sgsc.getNewState() != null) {
            LOGGER.trace("GSS Unlock: {}", sgsc.getNewState());
            getLockableGsm(sgsc.getNewState().getId()).unlockPersistent();
        }
        return sgsc;
    }

    @Override
    protected void sendNotification(CurrentState<Group, GroupState> anUpdatedState) {
        // Do NOT forward the notification on, since we are the ones who created it, it would come right back in.
        // stateNotificationGateway.groupStateChanged(anUpdatedState);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

    }
}

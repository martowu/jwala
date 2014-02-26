package com.siemens.cto.aem.service;

import com.siemens.cto.aem.common.User;
import com.siemens.cto.aem.persistence.dao.JvmDaoJpa;
import com.siemens.cto.aem.persistence.domain.Jvm;
import com.siemens.cto.aem.service.configuration.application.RecordNotAddedException;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityExistsException;
import java.util.ArrayList;
import java.util.List;

public class JvmInfoServiceImpl implements JvmInfoService {

    private final JvmDaoJpa jvmDao;

    public JvmInfoServiceImpl(JvmDaoJpa jvmDao) {
        this.jvmDao = jvmDao;
    }

    @Override
    public JvmInfo getJvmInfoById(Long id) {
        final Jvm jvm = jvmDao.findById(id);
        if (jvm != null) {
            return new JvmInfo(jvm.getId(), jvm.getName(), jvm.getHostName());
        } else {
            throw new RecordNotFoundException(id, Jvm.class.getSimpleName());
        }
    }

    @Override
    public List<JvmInfo> getAllJvmInfo() {
        final List<JvmInfo> jvmInfoList = new ArrayList<JvmInfo>();
        final List<Jvm> jvmList = jvmDao.findAll();
        for (Jvm jvm : jvmList) {
            jvmInfoList.add(new JvmInfo(jvm.getId(),
                                jvm.getName(),
                                jvm.getHostName()));

        }
        return jvmInfoList;
    }

    @Override
    @Transactional
    public void addJvmInfo(String jvmName, String hostName) {
        final Jvm jvm = new Jvm();
        jvm.setName(jvmName);
        jvm.setHostName(hostName);

        // Required by com.siemens.cto.aem.persistence.domain.AbstractEntity
        // TODO: Discuss with the team what module should handle this.
        final User user = new User("testUser", "");
        user.addToThread();

        try {
            jvmDao.add(jvm);
        } catch (EntityExistsException e) {
            throw new RecordNotAddedException(jvm.getClass(), jvm.getName(), e);
        }
    }

    @Override
    @Transactional
    public void updateJvmInfo(Long jvmId, String jvmName, String hostName) {
        final Jvm jvm = jvmDao.findById(jvmId);

        jvm.setName(jvmName);
        jvm.setHostName(hostName);

        // Required by com.siemens.cto.aem.persistence.domain.AbstractEntity
        // TODO: Discuss with the team what module should handle this.
        final User user = new User("testUser", "");
        user.addToThread();

        jvmDao.update(jvm);
    }

    @Override
    @Transactional
    public void deleteJvm(Long id) {
        jvmDao.remove(jvmDao.findById(id));
    }

}

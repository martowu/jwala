package com.cerner.jwala.common.dispatch;

import org.apache.commons.lang3.builder.EqualsBuilder;

import com.cerner.jwala.common.domain.model.group.Group;
import com.cerner.jwala.common.domain.model.user.User;
import com.cerner.jwala.common.request.webserver.ControlGroupWebServerRequest;

public class GroupWebServerDispatchCommand extends DispatchCommand {
    
    private final Group group;
    private final ControlGroupWebServerRequest request;
    private final User user;

    public GroupWebServerDispatchCommand(Group theGroup, ControlGroupWebServerRequest theCommand, User theUser) {
        group = theGroup;
        request = theCommand;
        user = theUser;
    }

    public Group getGroup() {
        return group;
    }

    public ControlGroupWebServerRequest getRequest() {
        return request;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "GroupWebServerDispatchCommand [group=" + group + ", request=" + request + ", user=" + user + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((request == null) ? 0 : request.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        GroupWebServerDispatchCommand rhs = (GroupWebServerDispatchCommand) obj;
        return new EqualsBuilder()
        .append(this.request, rhs.request)
        .append(this.group, rhs.group)
        .append(this.user,rhs.user)
        .isEquals();
    }

}

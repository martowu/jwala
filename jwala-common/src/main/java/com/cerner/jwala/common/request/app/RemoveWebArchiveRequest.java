package com.cerner.jwala.common.request.app;

import java.io.Serializable;

import com.cerner.jwala.common.domain.model.app.Application;
import com.cerner.jwala.common.exception.BadRequestException;
import com.cerner.jwala.common.request.Request;

@Deprecated
public class RemoveWebArchiveRequest implements Serializable, Request {

    private final Application application;
        
    public RemoveWebArchiveRequest(final Application application) {
        this.application = application;
    }

    @Override
    public void validate() {
        // intentionally empty
    }

    public Application getApplication() {
        return application;
    }
    
}

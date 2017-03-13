package com.cerner.jwala.ws.rest.v1.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.cerner.jwala.common.domain.model.fault.AemFaultType;
import com.cerner.jwala.common.domain.model.id.Identifier;
import com.cerner.jwala.common.exception.BadRequestException;

public abstract class AbstractIdsParameterProvider<T> {

    private final String badRequestMessage;

    public AbstractIdsParameterProvider(final String theBadRequestMessage) {
        badRequestMessage = theBadRequestMessage;
    }

    public Set<Identifier<T>> valueOf() {

        final Set<String> ids = getIds();

        if (isParameterPresent(ids)) {
            return parseParameters(ids);
        }

        return Collections.emptySet();
    }

    private boolean isParameterPresent(final Set<String> someIds) {
        return someIds != null;
    }

    private Set<Identifier<T>> parseParameters(final Set<String> someIds) {
        try {
            final Set<Identifier<T>> ids = new HashSet<>();
            for (final String id : someIds  ) {
                ids.add(new Identifier<T>(Long.valueOf(id)));
            }
            return ids;
        } catch (final NumberFormatException nfe) {
            throw new BadRequestException(AemFaultType.INVALID_IDENTIFIER,
                                          badRequestMessage,
                                          nfe);
        }
    }

    protected abstract Set<String> getIds();

}
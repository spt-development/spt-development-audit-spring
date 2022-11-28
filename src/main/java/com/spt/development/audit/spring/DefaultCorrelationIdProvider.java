package com.spt.development.audit.spring;

import com.spt.development.cid.CorrelationId;

/**
 * Default {@link CorrelationIdProvider} providing a wrapper around {@link CorrelationId}.
 */
public class DefaultCorrelationIdProvider implements CorrelationIdProvider {

    /**
     * Gets the current correlation Id.
     *
     * @return the current correlation Id.
     */
    @Override
    public String getCorrelationId() {
        return CorrelationId.get();
    }
}

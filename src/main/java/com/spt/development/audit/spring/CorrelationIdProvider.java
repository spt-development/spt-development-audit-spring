package com.spt.development.audit.spring;

/**
 * Wrapper for getting the current correlation ID.
 */
public interface CorrelationIdProvider {

    /**
     * Gets the current correlation Id.
     *
     * @return the current correlation Id.
     */
    String getCorrelationId();
}

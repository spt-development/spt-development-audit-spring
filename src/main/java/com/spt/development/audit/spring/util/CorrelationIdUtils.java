package com.spt.development.audit.spring.util;

import com.spt.development.cid.CorrelationId;

/**
 * Utility methods releated to the {@link CorrelationId}.
 */
public final class CorrelationIdUtils {
    private CorrelationIdUtils() {
    }

    /**
     * Adds the current correlation ID to the array of arguments (for logging).
     *
     * @param correlationId the correlation ID to add.
     * @param arguments the original set of arguments.
     *
     * @return a new array of arguments with the current correlation ID added at the beginning of the array.
     */
    public static Object[] addCorrelationIdToArguments(String correlationId, Object[] arguments) {
        final Object[] newArguments = new Object[arguments.length + 1];
        newArguments[0] = correlationId;

        System.arraycopy(arguments, 0, newArguments, 1, arguments.length);

        return newArguments;
    }
}

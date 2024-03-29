package com.spt.development.audit.spring;

import lombok.extern.slf4j.Slf4j;

/**
 * An implementation of {@link AuditEventWriter} that simply logs the audit event.
 */
@Slf4j
public class Slf4jAuditEventWriter implements AuditEventWriter {
    private final boolean includeCorrelationIdInLogs;
    private final CorrelationIdProvider correlationIdProvider;

    /**
     * Creates a new instance of the audit event writer. The log statements written by the audit event writer will
     * include the current correlation ID; see {@link Slf4jAuditEventWriter#Slf4jAuditEventWriter(boolean)} to disable
     * this behaviour.
     */
    public Slf4jAuditEventWriter() {
        this(true);
    }

    /**
     * Creates a new instance of the audit event writer.
     *
     * @param includeCorrelationIdInLogs a flag to determine whether the correlation ID should be explicitly included
     *                                   in the log statements written by this writer.
     */
    public Slf4jAuditEventWriter(boolean includeCorrelationIdInLogs) {
        this(includeCorrelationIdInLogs, new DefaultCorrelationIdProvider());
    }

    /**
     * Creates a new instance of the audit event writer.
     *
     * @param includeCorrelationIdInLogs a flag to determine whether the correlation ID should be explicitly included
     *                                   in the log statements written by this writer.
     * @param correlationIdProvider provider for getting the current correlationId.
     */
    public Slf4jAuditEventWriter(
            final boolean includeCorrelationIdInLogs,
            final CorrelationIdProvider correlationIdProvider) {

        this.includeCorrelationIdInLogs = includeCorrelationIdInLogs;
        this.correlationIdProvider = correlationIdProvider;
    }

    /**
     * Logs out the audit event using SLF4J.
     *
     * @param auditEvent the audit event to write.
     */
    @Override
    public void write(AuditEvent auditEvent) {
        if (includeCorrelationIdInLogs) {
            LOG.info("[{}] Audit event: {}", correlationIdProvider.getCorrelationId(), auditEvent);
            return;
        }
        LOG.info("Audit event: {}", auditEvent);
    }
}

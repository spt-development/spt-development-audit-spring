package com.spt.development.audit.spring;

import com.spt.development.cid.CorrelationId;
import lombok.extern.slf4j.Slf4j;

/**
 * An implementation of {@link AuditEventWriter} that simply logs the audit event.
 */
@Slf4j
public class Slf4jAuditEventWriter implements AuditEventWriter {

    /**
     * Logs out the audit event using SLF4J.
     *
     * @param auditEvent the audit event to write.
     */
    @Override
    public void write(AuditEvent auditEvent) {
        LOG.info("[{}] Audit event: {}", CorrelationId.get(), auditEvent);
    }
}

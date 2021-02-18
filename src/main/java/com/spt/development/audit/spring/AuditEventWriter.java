package com.spt.development.audit.spring;

/**
 * Writes audit events.
 */
public interface AuditEventWriter {

    /**
     * Writes an audit event.
     *
     * @param auditEvent the audit event to write.
     */
    void write(AuditEvent auditEvent);
}

package com.spt.development.audit.spring;

import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;

import static com.spt.development.audit.spring.util.CorrelationIdUtils.addCorrelationIdToArguments;

/**
 * An implementation of {@link AuditEventWriter} that adds the audit event to a JMS queue. The processing or storing of
 * the {@link AuditEvent} can then be performed asynchronously, possibly by a separate service - this is the recommended
 * implementation.
 */
@Slf4j
public class JmsAuditEventWriter extends TransactionAwareAuditEventWriter {
    private final String destinationName;
    private final JmsTemplate jmsTemplate;

    /**
     * Creates a new instance of the audit event writer. The log statements written by the audit event writer will
     * include the current correlation ID; see {@link JmsAuditEventWriter#JmsAuditEventWriter(boolean, String, JmsTemplate)}
     * to disable this behaviour.
     *
     * @param destinationName the name of the queue to write the audit events to.
     * @param jmsTemplate the JMS template to use to send the audit event JMS messages.
     */
    public JmsAuditEventWriter(final String destinationName, final JmsTemplate jmsTemplate) {
        this(true, destinationName, jmsTemplate);
    }

    /**
     * Creates a new instance of the audit event writer. If the <code>spt.cid.mdc.disabled</code> property is set to
     * <code>true</code>, the correlation ID will be included in any log statements.
     *
     * @param destinationName the name of the queue to write the audit events to.
     * @param jmsTemplate the JMS template to use to send the audit event JMS messages.
     */
    public JmsAuditEventWriter(
            final boolean includeCorrelationIdInLogs,
            final String destinationName,
            final JmsTemplate jmsTemplate) {
        this(includeCorrelationIdInLogs, destinationName, jmsTemplate, new DefaultCorrelationIdProvider());
    }

    /**
     * Creates a new instance of the audit event writer. If the <code>spt.cid.mdc.disabled</code> property is set to
     * <code>true</code>, the correlation ID will be included in any log statements.
     *
     * @param destinationName the name of the queue to write the audit events to.
     * @param jmsTemplate the JMS template to use to send the audit event JMS messages.
     * @param correlationIdProvider provider for getting the current correlationId.
     */
    public JmsAuditEventWriter(
            final boolean includeCorrelationIdInLogs,
            final String destinationName,
            final JmsTemplate jmsTemplate,
            final CorrelationIdProvider correlationIdProvider) {
        super(includeCorrelationIdInLogs, correlationIdProvider);

        this.destinationName = destinationName;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Converts the {@link AuditEvent} to JSON and adds it as a {@link jakarta.jms.TextMessage} to the configured JMS queue.
     *
     * @param auditEvent the {@link AuditEvent} to write.
     */
    @Override
    protected void doWrite(AuditEvent auditEvent) {
        debug("Adding audit event message to JMS queue: {}", auditEvent);

        jmsTemplate.send(destinationName, s -> {
            final Message message = s.createTextMessage(auditEvent.toJson());

            message.setJMSCorrelationID(getCorrelationId());

            return message;
        });
    }

    private void debug(String format, Object... arguments) {
        if (isIncludeCorrelationIdInLogs()) {
            LOG.debug("[{}] " + format, addCorrelationIdToArguments(getCorrelationId(), arguments));
            return;
        }
        LOG.debug(format, arguments);
    }
}

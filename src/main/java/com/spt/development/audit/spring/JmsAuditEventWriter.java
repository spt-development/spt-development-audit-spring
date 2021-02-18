package com.spt.development.audit.spring;

import com.spt.development.cid.CorrelationId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Message;

/**
 * An implementation of {@link AuditEventWriter} that adds the audit event to a JMS queue. The processing or storing of
 * the {@link AuditEvent} can then be performed asynchronously, possibly by a separate service - this is the recommended
 * implementation.
 */
@Slf4j
@AllArgsConstructor
public class JmsAuditEventWriter extends TransactionAwareAuditEventWriter {
    private final String destinationName;
    private final JmsTemplate jmsTemplate;

    /**
     * Converts the {@link AuditEvent} to JSON and adds it as a {@link javax.jms.TextMessage} to the configured JMS queue.
     *
     * @param auditEvent the {@link AuditEvent} to write.
     */
    @Override
    protected void doWrite(AuditEvent auditEvent) {
        LOG.debug("[{}] Adding audit event message to JMS queue: {}", CorrelationId.get(), auditEvent);

        jmsTemplate.send(destinationName, s -> {
            final Message message = s.createTextMessage(auditEvent.toJson());

            message.setJMSCorrelationID(CorrelationId.get());

            return message;
        });
    }
}

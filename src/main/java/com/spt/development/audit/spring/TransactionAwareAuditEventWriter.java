package com.spt.development.audit.spring;

import com.spt.development.cid.CorrelationId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.function.Consumer;

/**
 * Transaction aware {@link AuditEventWriter} which delays writing the audit event until the transaction is committed
 * if there is an active transaction when {@link AuditEventWriter#write(AuditEvent)} is called. This is useful if the
 * audit events are being written to a secondary store or a JMS queue for example that you only want written if your
 * primary (transactional) data store is successfully written to.
 */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TransactionAwareAuditEventWriter implements AuditEventWriter {
    private final TransactionSyncManFacade transactionSyncManFacade;

    /**
     * Creates a vanilla {@link TransactionAwareAuditEventWriter}.
     */
    protected TransactionAwareAuditEventWriter() {
        this(new TransactionSyncManFacade());
    }

    /**
     * Writes the audit event, delaying the writing until after the current transaction is committed if there is an
     * active transaction in flight.
     *
     * @param auditEvent the audit event to write.
     */
    @Override
    public void write(AuditEvent auditEvent) {
        if (transactionSyncManFacade.isTransactionActive()) {
            LOG.debug("[{}] Transaction active, audit event will be written when transaction commits: {}",
                    CorrelationId.get(), auditEvent);

            transactionSyncManFacade.register(new AuditEventTransactionSync(auditEvent, this::doWrite));

            return;
        }
        doWrite(auditEvent);
    }

    /**
     * Writes the {@link AuditEvent} either immediately or when the transaction commits if there is an active transaction
     * when {@link TransactionAwareAuditEventWriter#write(AuditEvent)} is called.
     *
     * @param auditEvent the {@link AuditEvent} to write.
     */
    protected abstract void doWrite(AuditEvent auditEvent);

    @AllArgsConstructor
    static class AuditEventTransactionSync implements TransactionSynchronization {
        private final AuditEvent auditEvent;
        private final Consumer<AuditEvent> onAuditEvent;

        @Override
        public void afterCommit() {

            try {
                onAuditEvent.accept(auditEvent);
            }
            catch (Exception ex) {
                // If an exception occurs log it and swallow. The whole idea of this class is that the audit events are
                // sent after the main work has been done, therefore we don't want an error reported to the user if
                // the auditing fails.
                //
                // All of the auditing data is logged in the log message below, so that *could* be used to manually repair
                // the audit log if necessary.
                LOG.error("[{}] Failed to write audit event: {}", CorrelationId.get(), auditEvent, ex);
            }
        }

        @Override
        public void afterCompletion(int status) {
            if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                LOG.info("[{}] Transaction was rolled back, discarding audit event: {}", CorrelationId.get(), auditEvent);
            }
        }
    }
}

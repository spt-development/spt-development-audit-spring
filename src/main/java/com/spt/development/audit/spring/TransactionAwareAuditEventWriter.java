package com.spt.development.audit.spring;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.spt.development.audit.spring.util.CorrelationIdUtils.addCorrelationIdToArguments;

/**
 * Transaction aware {@link AuditEventWriter} which delays writing the audit event until the transaction is committed
 * if there is an active transaction when {@link AuditEventWriter#write(AuditEvent)} is called. This is useful if the
 * audit events are being written to a secondary store or a JMS queue for example that you only want written if your
 * primary (transactional) data store is successfully written to.
 */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TransactionAwareAuditEventWriter implements AuditEventWriter {
    private final boolean includeCorrelationIdInLogs;
    private final CorrelationIdProvider correlationIdProvider;
    private final TransactionSyncManFacade transactionSyncManFacade;

    /**
     * Creates a {@link TransactionAwareAuditEventWriter}.
     *
     * @param includeCorrelationIdInLogs a flag to determine whether the correlation ID should be explicitly included
     *                                   in the log statements written by this writer.
     * @param correlationIdProvider provider for getting the current correlationId.
     */
    protected TransactionAwareAuditEventWriter(
            final boolean includeCorrelationIdInLogs,
            final CorrelationIdProvider correlationIdProvider) {
        this(includeCorrelationIdInLogs, correlationIdProvider, new TransactionSyncManFacade());
    }

    /**
     * Checks whether the correlation ID should be explicitly included in log statements or not.
     *
     * @return <code>true</code> if correlation IDs should be included in log statements.
     */
    protected boolean isIncludeCorrelationIdInLogs() {
        return includeCorrelationIdInLogs;
    }

    /**
     * Gets the current correlation Id.
     *
     * @return the current correlation Id.
     */
    protected String getCorrelationId() {
        return correlationIdProvider.getCorrelationId();
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
            debug("Transaction active, audit event will be written when transaction commits: {}", auditEvent);
            transactionSyncManFacade.register(new AuditEventTransactionSync(includeCorrelationIdInLogs, auditEvent, this::doWrite));

            return;
        }
        doWrite(auditEvent);
    }

    private void debug(String format, Object... arguments) {
        if (includeCorrelationIdInLogs) {
            LOG.debug("[{}] " + format, addCorrelationIdToArguments(getCorrelationId(), arguments));
            return;
        }
        LOG.debug(format, arguments);
    }

    /**
     * Writes the {@link AuditEvent} either immediately or when the transaction commits if there is an active transaction
     * when {@link TransactionAwareAuditEventWriter#write(AuditEvent)} is called.
     *
     * @param auditEvent the {@link AuditEvent} to write.
     */
    protected abstract void doWrite(AuditEvent auditEvent);

    @Slf4j
    @AllArgsConstructor
    static class AuditEventTransactionSync implements TransactionSynchronization {
        private final boolean includeCorrelationIdInLogs;
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
                error("Failed to write audit event: {}", auditEvent, ex);
            }
        }

        @Override
        public void afterCompletion(int status) {
            if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                info("Transaction was rolled back, discarding audit event: {}", auditEvent);
            }
        }

        void info(String format, Object... arguments) {
            log(LOG::info, format, arguments);
        }

        void error(String format, Object... arguments) {
            log(LOG::error, format, arguments);
        }

        private void log(BiConsumer<String, Object[]> log, String format, Object[] arguments) {
            if (includeCorrelationIdInLogs) {
                log.accept("[{}] " + format, addCorrelationIdToArguments(auditEvent.getCorrelationId(), arguments));
                return;
            }
            log.accept(format, arguments);
        }
    }
}

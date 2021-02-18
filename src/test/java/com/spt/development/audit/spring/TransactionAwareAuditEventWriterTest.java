package com.spt.development.audit.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.spt.development.cid.CorrelationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;

import static com.spt.development.test.LogbackUtil.verifyInfoLogging;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionAwareAuditEventWriterTest {
    private static final Gson GSON = new GsonBuilder().create();

    private interface TestData {
        String TYPE = "APPLICATION";
        String SUB_TYPE = "UPDATE";
        String CORRELATION_ID = "30f6f97d-6956-485b-b7e7-a6c689a3d6dd";
        String ID = "100";
        Map<String, String> DETAILS = Collections.singletonMap("json", "value");
        String USER_ID = "987";
        String USER_EMAIL = "tester@testing-times.com";
        String ORIGINATING_IP = "127.0.0.1";
        String SERVICE_ID = "testService";
        String SERVICE_VERSION = "1.0.1";
        String SERVER_HOST_NAME = "localhost";
        OffsetDateTime CREATED = OffsetDateTime.of(2020, 7, 19, 15, 29, 17, 0, ZoneOffset.UTC);
    }

    @BeforeEach
    void setUp() {
        CorrelationId.set(TestData.CORRELATION_ID);
    }

    @Test
    void onAuditEvent_validAuditEventInActiveTransaction_shouldWriteAuditEventOnCommitOfTransaction() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs();

        createAuditEventWriter(args).write(createAuditEvent());

        verifyAuditEventIsWritten(args.delegate);
    }

    @Test
    void onAuditEvent_validAuditEventInActiveTransactionNotCommitted_shouldNotWriteAuditEvent() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs();
        doAnswer(iom -> null).when(args.transactionSyncManFacade).register(any());

        createAuditEventWriter(args).write(createAuditEvent());

        verify(args.delegate, never()).write(any());
    }

    @Test
    void onAuditEvent_validAuditEventOutsideTransaction_shouldWriteAuditEventImmediately() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs();

        when(args.transactionSyncManFacade.isTransactionActive()).thenReturn(false);
        doAnswer(iom -> null).when(args.transactionSyncManFacade).register(any());

        createAuditEventWriter(args).write(createAuditEvent());

        verifyAuditEventIsWritten(args.delegate);
    }

    private void verifyAuditEventIsWritten(AuditEventWriter delegate) {
        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(delegate, times(1)).write(auditEventCaptor.capture());

        final AuditEvent auditEvent = auditEventCaptor.getValue();

        assertThat(auditEvent, is(notNullValue()));
        assertThat(auditEvent.getType(), is(TestData.TYPE));
        assertThat(auditEvent.getSubType(), is(TestData.SUB_TYPE));
        assertThat(auditEvent.getCorrelationId(), is(TestData.CORRELATION_ID));
        assertThat(auditEvent.getId(), is(TestData.ID));
        assertThat(GSON.fromJson(auditEvent.getDetails(), new TypeToken<Map<String, String>>(){}.getType()), is(TestData.DETAILS));
        assertThat(auditEvent.getUserId(), is(TestData.USER_ID));
        assertThat(auditEvent.getUsername(), is(TestData.USER_EMAIL));
        assertThat(auditEvent.getOriginatingIP(), is(TestData.ORIGINATING_IP));
        assertThat(auditEvent.getServiceId(), is(TestData.SERVICE_ID));
        assertThat(auditEvent.getServiceVersion(), is(TestData.SERVICE_VERSION));
        assertThat(auditEvent.getServerHostName(), is(TestData.SERVER_HOST_NAME));
        assertThat(auditEvent.getCreated(), is(TestData.CREATED));
    }

    @Test
    void onAuditEvent_auditWriteFailure_shouldSwallowException() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs();
        final TransactionAwareAuditEventWriter target = createAuditEventWriter(args);
        final AuditEvent auditEvent = createAuditEvent();

        doThrow(new RuntimeException("Test")).when(args.delegate).write(any());

        target.write(auditEvent);
    }

    @Test
    void onAuditEvent_transactionRolledBack_shouldLogThatTransactionIsRolledBack() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs();

        // Call afterCommit and afterCompletion as soon as register is called. This would normally happen
        // asynchronously when the transaction is committed.
        doAnswer(iom -> {
            iom.getArgument(0, TransactionSynchronization.class).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            return null;
        }).when(args.transactionSyncManFacade).register(any());

        final AuditEvent auditEvent = createAuditEvent();

        verifyInfoLogging(
                TransactionAwareAuditEventWriter.class,
                () -> {
                    createAuditEventWriter(args).write(auditEvent);
                    return null;
                },
                TestData.CORRELATION_ID,
                "Transaction was rolled back, discarding audit event",
                auditEvent.toString()
        );
    }

    private AuditEvent createAuditEvent() {
        return AuditEvent.builder()
                .type(TestData.TYPE)
                .subType(TestData.SUB_TYPE)
                .correlationId(TestData.CORRELATION_ID)
                .id(TestData.ID)
                .details(GSON.toJson(TestData.DETAILS))
                .userId(TestData.USER_ID)
                .username(TestData.USER_EMAIL)
                .originatingIP(TestData.ORIGINATING_IP)
                .serviceId(TestData.SERVICE_ID)
                .serviceVersion(TestData.SERVICE_VERSION)
                .serverHostName(TestData.SERVER_HOST_NAME)
                .created(TestData.CREATED)
                .build();
    }

    private TransactionAwareAuditEventWriter createAuditEventWriter(TransactionAwareAuditEventWriterArgs args) {
        return new TestTransactionAwareAuditEventWriter(args.delegate, args.transactionSyncManFacade);
    }

    @Test
    void constructor_defaultConstructor_shouldCreateNewInstanceWithTransactionSyncManFacade() {
        final TransactionAwareAuditEventWriter result = new TransactionAwareAuditEventWriter() {
            @Override
            protected void doWrite(AuditEvent auditEvent) {
                // NOOP
            }
        };

        final Object transactionSyncManFacade = ReflectionTestUtils.getField(result, "transactionSyncManFacade");

        assertThat(transactionSyncManFacade, is(notNullValue()));
        assertThat(transactionSyncManFacade, instanceOf(TransactionSyncManFacade.class));
    }

    private static class TransactionAwareAuditEventWriterArgs {
        AuditEventWriter delegate = Mockito.mock(AuditEventWriter.class);
        TransactionSyncManFacade transactionSyncManFacade = Mockito.mock(TransactionSyncManFacade.class);

        TransactionAwareAuditEventWriterArgs() {
            when(transactionSyncManFacade.isTransactionActive()).thenReturn(true);

            // Call afterCommit and afterCompletion as soon as register is called. This would normally happen
            // asynchronously when the transaction is committed.
            doAnswer(iom -> {
                iom.getArgument(0, TransactionSynchronization.class).afterCommit();
                iom.getArgument(0, TransactionSynchronization.class).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

                return null;
            }).when(transactionSyncManFacade).register(any());
        }
    }

    private static class TestTransactionAwareAuditEventWriter extends TransactionAwareAuditEventWriter {
        private final AuditEventWriter delegate;

        TestTransactionAwareAuditEventWriter(final AuditEventWriter auditEventWriter, final TransactionSyncManFacade transactionSyncManFacade) {
            super(transactionSyncManFacade);

            this.delegate = auditEventWriter;
        }

        @Override
        protected void doWrite(AuditEvent auditEvent) {
            delegate.write(auditEvent);
        }
    }
}
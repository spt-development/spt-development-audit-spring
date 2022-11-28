package com.spt.development.audit.spring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.spt.development.cid.CorrelationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;

import static com.spt.development.test.LogbackUtil.verifyErrorLogging;
import static com.spt.development.test.LogbackUtil.verifyInfoLogging;
import static com.spt.development.test.LogbackUtil.verifyLogging;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void write_validAuditEventInActiveTransaction_shouldWriteAuditEventOnCommitOfTransaction(boolean includeCorrelationIdInLogs) {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(includeCorrelationIdInLogs);

        createAuditEventWriter(args).write(createAuditEvent());

        verifyAuditEventIsWritten(args.delegate);
    }

    @Test
    void write_validAuditEvent_shouldDebugLogAuditEventWithoutCorrelationId() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(false);

        createAuditEventWriter(args).write(createAuditEvent());

        verifyLogging(
                TransactionAwareAuditEventWriter.class,
                () -> {
                    createAuditEventWriter(args).write(createAuditEvent());
                    return null;
                },
                (logs) -> {
                    final ILoggingEvent logEvent = logs.stream()
                            .filter(e -> e.getLevel() == Level.DEBUG)
                            .findFirst()
                            .orElse(null);

                    assertThat(logEvent, is(notNullValue()));

                    assertThat(logEvent.getFormattedMessage(), not(startsWith("[" + TestData.CORRELATION_ID + "]")));
                    assertThat(logEvent.getFormattedMessage(), containsString("Transaction active, audit event will be written when transaction commits:"));
                    assertThat(logEvent.getFormattedMessage(), containsString("type=" + TestData.TYPE));
                    assertThat(logEvent.getFormattedMessage(), containsString("subType=" + TestData.SUB_TYPE));
                    assertThat(logEvent.getFormattedMessage(), containsString("correlationId=" + TestData.CORRELATION_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("id=" + TestData.ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("details=" + GSON.toJson(TestData.DETAILS)));
                    assertThat(logEvent.getFormattedMessage(), containsString("userId=" + TestData.USER_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("username=" + TestData.USER_EMAIL));
                    assertThat(logEvent.getFormattedMessage(), containsString("originatingIP=" + TestData.ORIGINATING_IP));
                    assertThat(logEvent.getFormattedMessage(), containsString("serviceId=" + TestData.SERVICE_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("serviceVersion=" + TestData.SERVICE_VERSION));
                    assertThat(logEvent.getFormattedMessage(), containsString("serverHostName=" + TestData.SERVER_HOST_NAME));
                    assertThat(logEvent.getFormattedMessage(), containsString("created=" + TestData.CREATED));
                }
        );
    }

    @Test
    void write_validAuditEvent_shouldDebugLogAuditEventWithCorrelationId() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(true);

        createAuditEventWriter(args).write(createAuditEvent());

        verifyLogging(
                TransactionAwareAuditEventWriter.class,
                () -> {
                    createAuditEventWriter(args).write(createAuditEvent());
                    return null;
                },
                Level.DEBUG,
                "[" + TestData.CORRELATION_ID + "]",
                "Transaction active, audit event will be written when transaction commits:",
                "type=" + TestData.TYPE,
                "subType=" + TestData.SUB_TYPE,
                "correlationId=" + TestData.CORRELATION_ID,
                "id=" + TestData.ID,
                "details=" + GSON.toJson(TestData.DETAILS),
                "userId=" + TestData.USER_ID,
                "username=" + TestData.USER_EMAIL,
                "originatingIP=" + TestData.ORIGINATING_IP,
                "serviceId=" + TestData.SERVICE_ID,
                "serviceVersion=" + TestData.SERVICE_VERSION,
                "serverHostName=" + TestData.SERVER_HOST_NAME,
                "created=" + TestData.CREATED
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void write_validAuditEventInActiveTransactionNotCommitted_shouldNotWriteAuditEvent(boolean includeCorrelationIdInLogs) {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(includeCorrelationIdInLogs);
        doAnswer(iom -> null).when(args.transactionSyncManFacade).register(any());

        createAuditEventWriter(args).write(createAuditEvent());

        verify(args.delegate, never()).write(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void write_validAuditEventOutsideTransaction_shouldWriteAuditEventImmediately(boolean includeCorrelationIdInLogs) {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(includeCorrelationIdInLogs);

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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void write_auditWriteFailure_shouldSwallowException(boolean includeCorrelationIdInLogs) {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(includeCorrelationIdInLogs);
        final TransactionAwareAuditEventWriter target = createAuditEventWriter(args);
        final AuditEvent auditEvent = createAuditEvent();

        doThrow(new RuntimeException("Test")).when(args.delegate).write(any());

        target.write(auditEvent);
    }

    @Test
    void write_auditWriteFailure_shouldLogErrorWithoutCorrelationId() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(false);
        final TransactionAwareAuditEventWriter target = createAuditEventWriter(args);
        final AuditEvent auditEvent = createAuditEvent();

        doThrow(new RuntimeException("Test")).when(args.delegate).write(any());

        target.write(auditEvent);

        verifyLogging(
                TransactionAwareAuditEventWriter.class,
                () -> {
                    target.write(auditEvent);
                    return null;
                },
                (logs) -> {
                    final ILoggingEvent logEvent = logs.stream()
                            .filter(e -> e.getLevel() == Level.ERROR)
                            .findFirst()
                            .orElse(null);

                    assertThat(logEvent, is(notNullValue()));

                    assertThat(logEvent.getFormattedMessage(), not(startsWith("[" + TestData.CORRELATION_ID + "]")));
                    assertThat(logEvent.getFormattedMessage(), containsString("Failed to write audit event:"));
                    assertThat(logEvent.getFormattedMessage(), containsString("type=" + TestData.TYPE));
                    assertThat(logEvent.getFormattedMessage(), containsString("subType=" + TestData.SUB_TYPE));
                    assertThat(logEvent.getFormattedMessage(), containsString("correlationId=" + TestData.CORRELATION_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("id=" + TestData.ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("details=" + GSON.toJson(TestData.DETAILS)));
                    assertThat(logEvent.getFormattedMessage(), containsString("userId=" + TestData.USER_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("username=" + TestData.USER_EMAIL));
                    assertThat(logEvent.getFormattedMessage(), containsString("originatingIP=" + TestData.ORIGINATING_IP));
                    assertThat(logEvent.getFormattedMessage(), containsString("serviceId=" + TestData.SERVICE_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("serviceVersion=" + TestData.SERVICE_VERSION));
                    assertThat(logEvent.getFormattedMessage(), containsString("serverHostName=" + TestData.SERVER_HOST_NAME));
                    assertThat(logEvent.getFormattedMessage(), containsString("created=" + TestData.CREATED));
                }
        );
    }

    @Test
    void write_auditWriteFailure_shouldLogErrorWithCorrelationId() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(true);
        final TransactionAwareAuditEventWriter target = createAuditEventWriter(args);
        final AuditEvent auditEvent = createAuditEvent();

        doThrow(new RuntimeException("Test")).when(args.delegate).write(any());

        target.write(auditEvent);

        verifyErrorLogging(
                TransactionAwareAuditEventWriter.class,
                () -> {
                    target.write(auditEvent);
                    return null;
                },
                "[" + TestData.CORRELATION_ID + "]",
                "Failed to write audit event:",
                "type=" + TestData.TYPE,
                "subType=" + TestData.SUB_TYPE,
                "correlationId=" + TestData.CORRELATION_ID,
                "id=" + TestData.ID,
                "details=" + GSON.toJson(TestData.DETAILS),
                "userId=" + TestData.USER_ID,
                "username=" + TestData.USER_EMAIL,
                "originatingIP=" + TestData.ORIGINATING_IP,
                "serviceId=" + TestData.SERVICE_ID,
                "serviceVersion=" + TestData.SERVICE_VERSION,
                "serverHostName=" + TestData.SERVER_HOST_NAME,
                "created=" + TestData.CREATED
        );
    }

    @Test
    void write_transactionRolledBack_shouldLogThatTransactionIsRolledBackWithoutCorrelationId() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(false);

        // Call afterCommit and afterCompletion as soon as register is called. This would normally happen
        // asynchronously when the transaction is committed.
        doAnswer(iom -> {
            iom.getArgument(0, TransactionSynchronization.class).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            return null;
        }).when(args.transactionSyncManFacade).register(any());

        final AuditEvent auditEvent = createAuditEvent();

        verifyLogging(
                TransactionAwareAuditEventWriter.class,
                () -> {
                    createAuditEventWriter(args).write(auditEvent);
                    return null;
                },
                (logs) -> {
                    final ILoggingEvent logEvent = logs.stream()
                            .filter(e -> e.getLevel() == Level.INFO)
                            .findFirst()
                            .orElse(null);

                    assertThat(logEvent, is(notNullValue()));

                    assertThat(logEvent.getFormattedMessage(), not(startsWith("[" + TestData.CORRELATION_ID + "]")));
                    assertThat(logEvent.getFormattedMessage(), containsString("Transaction was rolled back, discarding audit event"));
                    assertThat(logEvent.getFormattedMessage(), containsString("type=" + TestData.TYPE));
                    assertThat(logEvent.getFormattedMessage(), containsString("subType=" + TestData.SUB_TYPE));
                    assertThat(logEvent.getFormattedMessage(), containsString("correlationId=" + TestData.CORRELATION_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("id=" + TestData.ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("details=" + GSON.toJson(TestData.DETAILS)));
                    assertThat(logEvent.getFormattedMessage(), containsString("userId=" + TestData.USER_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("username=" + TestData.USER_EMAIL));
                    assertThat(logEvent.getFormattedMessage(), containsString("originatingIP=" + TestData.ORIGINATING_IP));
                    assertThat(logEvent.getFormattedMessage(), containsString("serviceId=" + TestData.SERVICE_ID));
                    assertThat(logEvent.getFormattedMessage(), containsString("serviceVersion=" + TestData.SERVICE_VERSION));
                    assertThat(logEvent.getFormattedMessage(), containsString("serverHostName=" + TestData.SERVER_HOST_NAME));
                    assertThat(logEvent.getFormattedMessage(), containsString("created=" + TestData.CREATED));
                }
        );
    }

    @Test
    void write_transactionRolledBack_shouldLogThatTransactionIsRolledBackWithCorrelationId() {
        final TransactionAwareAuditEventWriterArgs args = new TransactionAwareAuditEventWriterArgs(true);

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
                "[" + TestData.CORRELATION_ID + "]",
                "Transaction was rolled back, discarding audit event",
                "type=" + TestData.TYPE,
                "subType=" + TestData.SUB_TYPE,
                "correlationId=" + TestData.CORRELATION_ID,
                "id=" + TestData.ID,
                "details=" + GSON.toJson(TestData.DETAILS),
                "userId=" + TestData.USER_ID,
                "username=" + TestData.USER_EMAIL,
                "originatingIP=" + TestData.ORIGINATING_IP,
                "serviceId=" + TestData.SERVICE_ID,
                "serviceVersion=" + TestData.SERVICE_VERSION,
                "serverHostName=" + TestData.SERVER_HOST_NAME,
                "created=" + TestData.CREATED
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
        return new TestTransactionAwareAuditEventWriter(args.includeCorrelationIdInLogs, args.delegate, args.transactionSyncManFacade);
    }

    @Test
    void constructor_defaultConstructor_shouldCreateNewInstanceWithTransactionSyncManFacade() {
        final TransactionAwareAuditEventWriter result = new TransactionAwareAuditEventWriter(true, new DefaultCorrelationIdProvider()) {
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
        boolean includeCorrelationIdInLogs;
        AuditEventWriter delegate = Mockito.mock(AuditEventWriter.class);
        TransactionSyncManFacade transactionSyncManFacade = Mockito.mock(TransactionSyncManFacade.class);

        TransactionAwareAuditEventWriterArgs(boolean includeCorrelationIdInLogs) {
            when(transactionSyncManFacade.isTransactionActive()).thenReturn(true);

            // Call afterCommit and afterCompletion as soon as register is called. This would normally happen
            // asynchronously when the transaction is committed.
            doAnswer(iom -> {
                iom.getArgument(0, TransactionSynchronization.class).afterCommit();
                iom.getArgument(0, TransactionSynchronization.class).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

                return null;
            }).when(transactionSyncManFacade).register(any());

            this.includeCorrelationIdInLogs = includeCorrelationIdInLogs;
        }
    }

    private static class TestTransactionAwareAuditEventWriter extends TransactionAwareAuditEventWriter {
        private final AuditEventWriter delegate;

        TestTransactionAwareAuditEventWriter(
                final boolean includeCorrelationIdInLogs,
                final AuditEventWriter auditEventWriter,
                final TransactionSyncManFacade transactionSyncManFacade
        ) {
            super(includeCorrelationIdInLogs, new DefaultCorrelationIdProvider(), transactionSyncManFacade);

            this.delegate = auditEventWriter;
        }

        @Override
        protected void doWrite(AuditEvent auditEvent) {
            delegate.write(auditEvent);
        }
    }
}
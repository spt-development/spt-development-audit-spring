package com.spt.development.audit.spring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spt.development.cid.CorrelationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;

import static com.spt.development.test.LogbackUtil.verifyInfoLogging;
import static com.spt.development.test.LogbackUtil.verifyLogging;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

class Slf4jAuditEventWriterTest {
    private static final Gson GSON = new GsonBuilder().create();

    private static class TestData {
        static final String TYPE = "APPLICATION";
        static final String SUB_TYPE = "UPDATE";
        static final String CORRELATION_ID = "30f6f97d-6956-485b-b7e7-a6c689a3d6dd";
        static final String ID = "100";
        static final Map<String, String> DETAILS = Collections.singletonMap("json", "value");
        static final String USER_ID = "987";
        static final String USER_EMAIL = "tester@testing-times.com";
        static final String ORIGINATING_IP = "127.0.0.1";
        static final String SERVICE_ID = "testService";
        static final String SERVICE_VERSION = "1.0.1";
        static final String SERVER_HOST_NAME = "localhost";
        static final OffsetDateTime CREATED = OffsetDateTime.of(2020, 7, 19, 15, 29, 17, 0, ZoneOffset.UTC);
    }

    @BeforeEach
    void setUp() {
        CorrelationId.set(TestData.CORRELATION_ID);
    }

    @Test
    void write_validAuditEvent_shouldLogAuditEventWithoutCorrelationId() {
        verifyLogging(
                Slf4jAuditEventWriter.class,
                () -> {
                    createWriter(false).write(createAuditEvent());
                    return null;
                },
                (logs) -> {
                    final ILoggingEvent logEvent = logs.stream()
                            .filter(e -> e.getLevel() == Level.INFO)
                            .findFirst()
                            .orElse(null);

                    assertThat(logEvent, is(notNullValue()));

                    assertThat(logEvent.getFormattedMessage(), not(startsWith("[" + TestData.CORRELATION_ID + "]")));
                    assertThat(logEvent.getFormattedMessage(), containsString("Audit event:"));
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
    void write_validAuditEvent_shouldLogAuditEventWithCorrelationId() {
        verifyInfoLogging(
                Slf4jAuditEventWriter.class,
                () -> {
                    createWriter(true).write(createAuditEvent());
                    return null;
                },
                "[" + TestData.CORRELATION_ID + "]",
                "Audit event:",
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

    private Slf4jAuditEventWriter createWriter(boolean includeCorrelationIdInLogs) {
        return includeCorrelationIdInLogs ? new Slf4jAuditEventWriter() : new Slf4jAuditEventWriter(false);
    }
}
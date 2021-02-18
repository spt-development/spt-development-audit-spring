package com.spt.development.audit.spring;

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

class Slf4jAuditEventWriterTest {
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
    void write_validAuditEvent_shouldLogAuditEvent() {
        verifyInfoLogging(
                Slf4jAuditEventWriter.class,
                () -> {
                    createWriter().write(createAuditEvent());
                    return null;
                },
                TestData.CORRELATION_ID,
                "Audit event:",
                TestData.TYPE,
                TestData.SUB_TYPE,
                TestData.ID,
                GSON.toJson(TestData.DETAILS),
                TestData.USER_ID,
                TestData.USER_EMAIL,
                TestData.ORIGINATING_IP,
                TestData.SERVICE_ID,
                TestData.SERVICE_VERSION,
                TestData.SERVER_HOST_NAME,
                TestData.CREATED.toString()
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

    private Slf4jAuditEventWriter createWriter() {
        return new Slf4jAuditEventWriter();
    }
}
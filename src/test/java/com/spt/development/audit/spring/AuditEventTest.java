package com.spt.development.audit.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class AuditEventTest {
    private static final Gson GSON = new GsonBuilder().create();

    private static final class TestData {
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

    @Test
    void fromJson_auditEventToJson_shouldReturnAuditEventEqualToOrig() {
        final AuditEvent result = AuditEvent.fromJson(createAuditEvent().toJson());

        assertThat(result, is(createAuditEvent()));
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
}
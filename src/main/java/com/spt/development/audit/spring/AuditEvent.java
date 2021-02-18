package com.spt.development.audit.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/**
 * Class to encapsulate the audit events generated.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditEvent {
    private static final Gson GSON = new GsonBuilder().create();

    private String type;
    private String subType;
    private String correlationId;
    private String id;
    private String details;
    private String userId;
    private String username;
    private String originatingIP;
    private String serviceId;
    private String serviceVersion;
    private String serverHostName;
    private OffsetDateTime created;

    /**
     * Converts the audit event to JSON.
     *
     * @return a JSON representation of this audit event.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Converts the JSON to an {@link AuditEvent}.
     *
     * @param json the JSON to deserialize the {@link AuditEvent} from.
     *
     * @return a new {@link AuditEvent}.
     */
    public static AuditEvent fromJson(String json) {
        return GSON.fromJson(json, AuditEvent.class);
    }
}

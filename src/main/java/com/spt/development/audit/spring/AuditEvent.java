package com.spt.development.audit.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Class to encapsulate the audit events generated.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditEvent {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    OffsetDateTime.class,
                    (JsonSerializer<OffsetDateTime>) (src, typeOfSrc, context) ->
                            new JsonPrimitive(ISO_OFFSET_DATE_TIME.format(src))
            )
            .registerTypeAdapter(
                    OffsetDateTime.class,
                    (JsonDeserializer<OffsetDateTime>) (json, typeOfT, context) ->
                            ISO_OFFSET_DATE_TIME.parse(json.getAsString(), OffsetDateTime::from)
            )
            .create();

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

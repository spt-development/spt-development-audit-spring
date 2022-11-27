package com.spt.development.audit.spring.aop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spt.development.audit.spring.AuditEvent;
import com.spt.development.audit.spring.AuditEventWriter;
import com.spt.development.audit.spring.Audited;
import com.spt.development.audit.spring.CorrelationIdProvider;
import com.spt.development.audit.spring.DefaultCorrelationIdProvider;
import com.spt.development.audit.spring.security.AuthenticationAdapter;
import com.spt.development.audit.spring.security.AuthenticationAdapterFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.spt.development.audit.spring.util.CorrelationIdUtils.addCorrelationIdToArguments;
import static com.spt.development.audit.spring.util.HttpRequestUtils.getClientIpAddress;

/**
 * Aspect that implements auditing for methods annotated with the {@link Audited} annotation.
 */
@Slf4j
@Aspect
public class Auditor {
    private static final Gson GSON = new GsonBuilder().create();

    private static final String DEFAULT_DETAILS_KEY = "DETAILS";

    private final String appName;
    private final String appVersion;
    private final LocalhostFacade localhostFacade;
    private final AuditEventWriter auditEventWriter;
    private final boolean includeCorrelationIdInLogs;
    private final CorrelationIdProvider correlationIdProvider;
    private final AuthenticationAdapterFactory authenticationAdapterFactory;

    /**
     * Creates a new instance of the aspect.
     *
     * @param appName the name of the application that the auditing is for.
     * @param appVersion the version of the application that the auditing is for.
     * @param auditEventWriter the audit event writer that writes the audit logs.
     * @param authenticationAdapterFactory a factory for creating an adapter that is used to retrieve details about the
     *                                     currently authenticated user.
     */
    public Auditor(
            final String appName,
            final String appVersion,
            final AuditEventWriter auditEventWriter,
            final AuthenticationAdapterFactory authenticationAdapterFactory) {
        this(appName, appVersion, auditEventWriter, true, authenticationAdapterFactory);
    }

    /**
     * Creates a new instance of the aspect.
     *
     * @param appName the name of the application that the auditing is for.
     * @param appVersion the version of the application that the auditing is for.
     * @param auditEventWriter the audit event writer that writes the audit logs.
     * @param includeCorrelationIdInLogs a flag to determine whether the correlation ID should be explicitly included
     *                                   in the log statements output by the aspect.
     * @param authenticationAdapterFactory a factory for creating an adapter that is used to retrieve details about the
     *                                     currently authenticated user.
     */
    public Auditor(
            final String appName,
            final String appVersion,
            final AuditEventWriter auditEventWriter,
            final boolean includeCorrelationIdInLogs,
            final AuthenticationAdapterFactory authenticationAdapterFactory) {
        this(appName, appVersion, auditEventWriter, includeCorrelationIdInLogs, new DefaultCorrelationIdProvider(), authenticationAdapterFactory);
    }

    /**
     * Creates a new instance of the aspect.
     *
     * @param appName the name of the application that the auditing is for.
     * @param appVersion the version of the application that the auditing is for.
     * @param auditEventWriter the audit event writer that writes the audit logs.
     * @param includeCorrelationIdInLogs a flag to determine whether the correlation ID should be explicitly included
     *                                   in the log statements output by the aspect.
     * @param correlationIdProvider provider for getting the current correlationId.
     * @param authenticationAdapterFactory a factory for creating an adapter that is used to retrieve details about the
     *                                     currently authenticated user.
     */
    public Auditor(
            final String appName,
            final String appVersion,
            final AuditEventWriter auditEventWriter,
            final boolean includeCorrelationIdInLogs,
            final CorrelationIdProvider correlationIdProvider,
            final AuthenticationAdapterFactory authenticationAdapterFactory) {
        this(appName, appVersion, new LocalhostFacade(), auditEventWriter, includeCorrelationIdInLogs,
                correlationIdProvider, authenticationAdapterFactory);
    }

    /**
     * Creates a new instance of the aspect.
     *
     * @param appName the name of the application that the auditing is for.
     * @param appVersion the version of the application that the auditing is for.
     * @param localhostFacade a facade used to retrieve the hostname of the machine the application is running on.
     * @param auditEventWriter the audit event writer that writes the audit logs.
     * @param includeCorrelationIdInLogs a flag to determine whether the correlation ID should be explicitly included
     *                                   in the log statements output by the aspect.
     * @param correlationIdProvider provider for getting the current correlationId.
     * @param authenticationAdapterFactory a factory for creating an adapter that is used to retrieve details about the
     *                                     currently authenticated user.
     */
    Auditor(
            final String appName,
            final String appVersion,
            final LocalhostFacade localhostFacade,
            final AuditEventWriter auditEventWriter,
            final boolean includeCorrelationIdInLogs,
            final CorrelationIdProvider correlationIdProvider,
            final AuthenticationAdapterFactory authenticationAdapterFactory) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.localhostFacade = localhostFacade;
        this.auditEventWriter = auditEventWriter;
        this.includeCorrelationIdInLogs = includeCorrelationIdInLogs;
        this.correlationIdProvider = correlationIdProvider;
        this.authenticationAdapterFactory = authenticationAdapterFactory;
    }

    /**
     * Generates audit logs for methods annotated with the {@link Audited annotation}. It is envisaged that this will
     * predominantly be methods belonging to classes annotated with the {@link org.springframework.stereotype.Service}
     * annotation, however there is nothing in the implementation that prevents other methods being annotated with the
     * {@link Audited} annotation.
     *
     * <p>
     * For details of usage, see the
     * <a href="https://github.com/spt-development/spt-development-test/blob/main/README.md">README</a>.
     * </p>
     *
     * @param point the aspect join point required for implementing a {@link Around} aspect.
     *
     * @return the value returned from the audited method.
     *
     * @throws Throwable thrown if the audited method throws a {@link Throwable}.
     */
    @Around("@annotation(com.spt.development.audit.spring.Audited)")
    public Object audit(ProceedingJoinPoint point) throws Throwable {
        final Object result = point.proceed();

        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Audited audited = AnnotatedElementUtils.getMergedAnnotation(signature.getMethod(), Audited.class);

        if (Audited.NONE.equals(Optional.of(audited).map(Audited::type).orElse(Audited.NONE))) {
            throw new IllegalStateException("Programming error: @Audited annotation must have type set");
        }
        audit(audited, result, signature, point.getArgs());

        return result;
    }

    private void audit(Audited audited, Object result, MethodSignature signature, Object[] args) {
        final AuthenticationAdapter authentication = authenticationAdapterFactory.createAdapter();

        final Parameter[] parameters = signature.getMethod().getParameters();
        final Audited.Id auditedId = AnnotatedElementUtils.getMergedAnnotation(signature.getMethod(), Audited.Id.class);

        final AuditEvent auditEvent = AuditEvent.builder()
                .type(audited.type())
                .subType(audited.subType())
                .correlationId(correlationIdProvider.getCorrelationId())
                .id(
                        auditedId == null ?
                                getIdFromFirstAnnotatedMethodParameter(parameters, args) :
                                getIdFromAnnotatedValue("Return value", result, auditedId)
                )
                .details(getDetailsFromAnnotatedParametersAsJson(parameters, args))
                .userId(authentication.getUserId())
                .username(authentication.getUsername())
                .originatingIP(getClientIpAddress())
                .serviceId(appName)
                .serviceVersion(appVersion)
                .serverHostName(getServerHostName())
                .created(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        onAuditEvent(auditEvent);
    }

    private String getIdFromFirstAnnotatedMethodParameter(Parameter[] parameters, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            final Audited.Id auditedId = AnnotatedElementUtils.getMergedAnnotation(parameters[i], Audited.Id.class);

            if (auditedId != null) {
                return getIdFromAnnotatedValue("Parameter " + (i + 1), args[i], auditedId);
            }
        }
        debug("No parameters annotated with @Audited.Id annotation");

        return null;
    }

    private String getIdFromAnnotatedValue(String annotationPosition, Object value, Audited.Id auditedId) {
        if (value == null) {
            warn("{} was annotated with @Audit.Id annotation but is null", annotationPosition);

            return null;
        }

        if (StringUtils.isEmpty(auditedId.field())) {
            return value.toString();
        }
        return readIdFromValue(annotationPosition, value, auditedId.field());
    }

    private String readIdFromValue(String annotationPosition, Object value, String fieldName) {
        try {
            final Field field = value.getClass().getDeclaredField(fieldName);
            final Object fieldValue = makeAccessibleAndGetField(field, value);

            if (fieldValue == null) {
                warn("{} was annotated with @Audit.Id(field = \"{}\") annotation but the '{}' field is null",
                        annotationPosition, fieldName, fieldName);

                return null;
            }
            return fieldValue.toString();
        }
        catch (NoSuchFieldException ex) {
            throw new IllegalStateException(
                    String.format("Programming error: %s of type: %s was annotated with @Audited.Id(field = \"%s\"), " +
                                    "but no field with the name: '%s' could be found",
                                  annotationPosition, value.getClass(), fieldName, fieldName),
                    ex
            );
        }
    }

    private Object makeAccessibleAndGetField(Field field, Object obj) {
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, obj);
    }

    private String getDetailsFromAnnotatedParametersAsJson(Parameter[] parameters, Object[] args) {
        final Map<String, Object> details = getDetailsFromAnnotatedParameters(parameters, args);

        if (details.isEmpty()) {
            return null;
        }
        return auditDetailsToJson(details);
    }

    private Map<String, Object> getDetailsFromAnnotatedParameters(Parameter[] parameters, Object[] args) {
        final Map<String, Object> details = new HashMap<>();

        for (int i = 0; i < parameters.length; i++) {
            final Audited.Detail auditedDetail = AnnotatedElementUtils.getMergedAnnotation(parameters[i], Audited.Detail.class);

            if (auditedDetail != null) {
                final String detailName = auditedDetail.name();

                if (StringUtils.isEmpty(detailName) && !details.isEmpty()) {
                    throw new IllegalStateException(
                            String.format("Programming error: If multiple method parameters are annotated with @Audited.Detail, " +
                                    "they must all have their name set. Name was not set for parameter %d", i)
                    );
                }
                details.put(StringUtils.isEmpty(detailName) ? DEFAULT_DETAILS_KEY : detailName, args[i]);
            }
        }
        return details;
    }

    private String auditDetailsToJson(Map<String, Object> details) {
        if (details.containsKey(DEFAULT_DETAILS_KEY)) {
            return GSON.toJson(details.get(DEFAULT_DETAILS_KEY));
        }
        return GSON.toJson(details);
    }

    private String getServerHostName() {
        try {
            return localhostFacade.getServerHostName();
        }
        catch (UnknownHostException ex) {
            warn("Failed to determine server host name for auditing purposes", ex);
        }
        return null;
    }

    private void onAuditEvent(AuditEvent auditEvent) {
        debug("Generated audit event: {}", auditEvent);

        try {
            auditEventWriter.write(auditEvent);
        }
        catch (Throwable t) {
            error("Failed to send audit event: {}", auditEvent);
        }
    }

    private void debug(String format, Object... arguments) {
        log(LOG::debug, format, arguments);
    }

    private void warn(String format, Object... arguments) {
        log(LOG::warn, format, arguments);
    }

    private void error(String format, Object... arguments) {
        log(LOG::error, format, arguments);
    }

    private void log(BiConsumer<String, Object[]> log, String format, Object[] arguments) {
        if (includeCorrelationIdInLogs) {
            log.accept("[{}] " + format, addCorrelationIdToArguments(correlationIdProvider.getCorrelationId(), arguments));
            return;
        }
        log.accept(format, arguments);
    }
}

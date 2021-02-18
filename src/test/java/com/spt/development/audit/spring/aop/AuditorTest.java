package com.spt.development.audit.spring.aop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spt.development.audit.spring.AuditEvent;
import com.spt.development.audit.spring.AuditEventWriter;
import com.spt.development.audit.spring.Audited;
import com.spt.development.audit.spring.security.AuthenticationAdapter;
import com.spt.development.audit.spring.security.AuthenticationAdapterFactory;
import com.spt.development.cid.CorrelationId;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.spt.development.test.LogbackUtil.verifyErrorLogging;
import static com.spt.development.test.LogbackUtil.verifyWarnLogging;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditorTest {
    private static final Gson GSON = new GsonBuilder().create();

    private interface TestData {
        String APP_NAME = "Test Application";
        String VERSION = "v1.0.0";
        String CORRELATION_ID = "b3015502-3b43-4012-95c2-1021f0aa9da9";

        String ID = "9879798";
        String USER_ID = "87689";
        String USERNAME = "testuser@testing-times.com";
        String SERVER_HOST_NAME = "spt-main-host";
        String ORIGINATING_IP = "127.9.9.9";
        String TYPE = "APPLICATION";
        String SUB_TYPE = "TEST_SUB_TYPE";

        String RESULT = "Success!";
        String METHOD = "test";

        String ARG1 = "TestArg";
        String ARG2 = "TestArg2";
    }

    @BeforeEach
    void setUp() {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(TestData.ORIGINATING_IP);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        CorrelationId.set(TestData.CORRELATION_ID);
    }

    @Test
    void audit_joinPointWithReturnValue_shouldReturnJoinPointsReturnValue() throws Throwable {
        final Object result = createAuditor().audit(mockJoinPoint(new Object[0]));

        assertThat(result, is(TestData.RESULT));
    }

    @Test
    void audit_auditedMethodWithoutTypeSet_shouldThrowException() throws Throwable {
        final Auditor target = createAuditor();
        final ProceedingJoinPoint joinPoint = mockJoinPoint("testTypeNotSet", new Object[0]);

        final IllegalStateException result = assertThrows(IllegalStateException.class, () -> target.audit(joinPoint));

        assertThat(result, is(notNullValue()));
        assertThat(result.getMessage(), is("Programming error: @Audited annotation must have type set"));
    }

    @Test
    void audit_auditedMethod_shouldSendAuditEvent() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        createAuditor(args).audit(mockJoinPoint(new Object[0]));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getType(), is(TestData.TYPE));
        assertThat(auditEventCaptor.getValue().getSubType(), is(TestData.SUB_TYPE));
        assertThat(auditEventCaptor.getValue().getCorrelationId(), is(TestData.CORRELATION_ID));
        assertThat(auditEventCaptor.getValue().getId(), is(nullValue()));
        assertThat(auditEventCaptor.getValue().getDetails(), is(nullValue()));
        assertThat(auditEventCaptor.getValue().getUserId(), is(TestData.USER_ID));
        assertThat(auditEventCaptor.getValue().getUsername(), is(TestData.USERNAME));
        assertThat(auditEventCaptor.getValue().getOriginatingIP(), is(TestData.ORIGINATING_IP));
        assertThat(auditEventCaptor.getValue().getServiceId(), is(TestData.APP_NAME));
        assertThat(auditEventCaptor.getValue().getServiceVersion(), is(TestData.VERSION));
        assertThat(auditEventCaptor.getValue().getServerHostName(), is(TestData.SERVER_HOST_NAME));
        assertThat(auditEventCaptor.getValue().getCreated(), is(notNullValue()));
    }

    @Test
    void audit_auditedMethodWithIdParameter_shouldSendAuditEventWithIdSet() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        createAuditor(args).audit(mockJoinPoint(new Object[] { Long.parseLong(TestData.ID) }, long.class));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getId(), is(TestData.ID));
    }

    @Test
    void audit_auditedMethodWithIdFieldOfParameter_shouldSendAuditEventWithIdSet() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        createAuditor(args).audit(mockJoinPoint(new Object[] { new TestIdParameter() }));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getId(), is(TestData.ID));
    }

    @Test
    void audit_auditedMethodWithIdReturnType_shouldSendAuditEventWithIdSet() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        createAuditor(args).audit(mockJoinPoint("testIdReturned", new Object[0]));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        // Id is set to result because this is what the mock join point is set up to return from proceed
        assertThat(auditEventCaptor.getValue().getId(), is(TestData.RESULT));
    }

    @Test
    void audit_auditedMethodWithIdFieldOfReturnType_shouldSendAuditEventWithIdSet() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        final ProceedingJoinPoint joinPoint = mockJoinPoint("testIdFromReturnedObject", new Object[0]);
        when(joinPoint.proceed()).thenReturn(new TestIdParameter());

        createAuditor(args).audit(joinPoint);

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getId(), is(TestData.ID));
    }

    @Test
    void audit_auditedMethodWithNullIdParameter_shouldSendAuditEventWithNullId() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        createAuditor(args).audit(mockJoinPoint(new Object[] { null }, TestIdParameter.class));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getId(), is(nullValue()));
    }


    @Test
    void audit_auditedMethodWithNullIdParameter_shouldLogWarning() throws Throwable {
        verifyWarnLogging(
                Auditor.class,
                () -> {
                    try {
                        createAuditor().audit(mockJoinPoint(new Object[] { null }, TestIdParameter.class));
                    }
                    catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    return null;
                },
                TestData.CORRELATION_ID,
                "Parameter 1 was annotated with @Audit.Id annotation but is null"
        );
    }

    @Test
    void audit_auditedMethodWithNullIdFieldOfParameter_shouldSendAuditEventWithNullId() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        createAuditor(args).audit(mockJoinPoint(new Object[] { new TestIdParameter(null) }));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getId(), is(nullValue()));
    }

    @Test
    void audit_auditedMethodWithoutUnknownIdField_shouldThrowException() throws Throwable {
        final Auditor target = createAuditor();
        final ProceedingJoinPoint joinPoint = mockJoinPoint("testUnknownIdField", new Object[] { new Object() }, Object.class);

        final IllegalStateException result = assertThrows(IllegalStateException.class, () -> target.audit(joinPoint));

        assertThat(result, is(notNullValue()));
        assertThat(result.getMessage(), containsString("Programming error"));
        assertThat(result.getMessage(), containsString("no field with the name"));
        assertThat(result.getMessage(), containsString("could be found"));
    }

    @Test
    void audit_auditedMethodWithDetailParameter_shouldSendAuditEventWithDetailsSet() throws Throwable {
        final AuditorArgs args = new AuditorArgs();
        final TestDetailParameter parameter = new TestDetailParameter("field1", "field2");

        createAuditor(args).audit(mockJoinPoint(new Object[] { parameter }));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getDetails(), is(GSON.toJson(parameter)));
    }

    @Test
    void audit_auditedMethodWithMultipleDetailParameters_shouldSendAuditEventWithDetailsSet() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        final TestDetailParameter parameter1 = new TestDetailParameter("field1", "field2");
        final TestDetailParameter parameter2 = new TestDetailParameter("field3", "field4");

        createAuditor(args).audit(mockJoinPoint(new Object[] { parameter1, parameter2 }));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        final Map<String, TestDetailParameter> expectedDetails = MapUtils.putAll(new HashMap<>(), new Object[] {
                new DefaultMapEntry<>(TestData.ARG1, parameter1),
                new DefaultMapEntry<>(TestData.ARG2, parameter2)
        });

        assertThat(auditEventCaptor.getValue().getDetails(), is(GSON.toJson(expectedDetails)));
    }

    @Test
    void audit_auditedMethodMultipleDetailParametersNoNames_shouldThrowException() throws Throwable {
        final Auditor target = createAuditor();

        final TestDetailParameter parameter1 = new TestDetailParameter("field1", "field2");
        final TestDetailParameter parameter2 = new TestDetailParameter("field3", "field4");

        final ProceedingJoinPoint joinPoint = mockJoinPoint(
                "testMultipleDetailsNoNames", new Object[] { parameter1, parameter2 },
                TestDetailParameter.class, TestDetailParameter.class
        );

        final IllegalStateException result = assertThrows(IllegalStateException.class, () -> target.audit(joinPoint));

        assertThat(result, is(notNullValue()));
        assertThat(result.getMessage(), containsString("Programming error: If multiple method parameters are annotated"));
        assertThat(result.getMessage(), containsString("with @Audited.Detail, they must all have their name set"));
    }

    @Test
    void audit_auditedMethodFailsToGetServerHostName_shouldSendAuditEventWithNullServerHostName() throws Throwable {
        final AuditorArgs args = new AuditorArgs();

        when(args.localhostFacade.getServerHostName()).thenThrow(new UnknownHostException("test"));

        createAuditor(args).audit(mockJoinPoint(new Object[0]));

        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(args.auditEventWriter, times(1)).write(auditEventCaptor.capture());

        assertThat(auditEventCaptor.getValue().getServerHostName(), is(nullValue()));
    }

    @Test
    void audit_auditedMethodFailsToSendAuditEvent_shouldLogError() throws Throwable {
        final AuditorArgs args = new AuditorArgs();
        final ProceedingJoinPoint joinPoint = mockJoinPoint(new Object[0]);

        doThrow(new RuntimeException("Test")).when(args.auditEventWriter).write(any());

        verifyErrorLogging(
                Auditor.class,
                () -> {
                    try {
                        createAuditor(args).audit(joinPoint);
                    }
                    catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    return null;
                },
                CorrelationId.get(),
                "Failed to send audit event"
        );
    }

    private ProceedingJoinPoint mockJoinPoint(Object[] args) throws Throwable {
        final Class<?>[] argTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class<?>[]::new);

        return mockJoinPoint(args, argTypes);
    }

    private ProceedingJoinPoint mockJoinPoint(Object[] args, Class<?>... argTypes) throws Throwable {
        return mockJoinPoint(TestData.METHOD, args, argTypes);
    }

    private ProceedingJoinPoint mockJoinPoint(String methodName, Object[] args, Class<?>... argTypes) throws Throwable {
        final ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        final MethodSignature methodSignature = Mockito.mock(MethodSignature.class);

        when(joinPoint.proceed()).thenReturn(TestData.RESULT);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(args);

        when(methodSignature.getMethod()).thenReturn(TestTarget.class.getMethod(methodName, argTypes));

        return joinPoint;
    }

    private Auditor createAuditor() throws UnknownHostException {
        return createAuditor(new AuditorArgs());
    }

    private Auditor createAuditor(AuditorArgs args) {
        return new Auditor(args.appName, args.appVersion, args.localhostFacade, args.auditEventWriter,
                args.authenticationAdapterFactory);
    }

    @Test
    void constructor_publicConstructor_shouldCreateNewInstanceWithLocalhostFacade() {
        final Auditor result = new Auditor(null, null, null, null);

        final Object localhostFacade = ReflectionTestUtils.getField(result, "localhostFacade");

        assertThat(localhostFacade, is(notNullValue()));
        assertThat(localhostFacade, instanceOf(LocalhostFacade.class));
    }

    private static class AuditorArgs {
        String appName = TestData.APP_NAME;
        String appVersion = TestData.VERSION;
        LocalhostFacade localhostFacade = Mockito.mock(LocalhostFacade.class);
        AuditEventWriter auditEventWriter = Mockito.mock(AuditEventWriter.class);
        AuthenticationAdapterFactory authenticationAdapterFactory = Mockito.mock(AuthenticationAdapterFactory.class);

        AuditorArgs() throws UnknownHostException {
            final AuthenticationAdapter authenticationAdapter = Mockito.mock(AuthenticationAdapter.class);

            when(localhostFacade.getServerHostName()).thenReturn(TestData.SERVER_HOST_NAME);
            when(authenticationAdapterFactory.createAdapter()).thenReturn(authenticationAdapter);
            when(authenticationAdapter.getUserId()).thenReturn(TestData.USER_ID);
            when(authenticationAdapter.getUsername()).thenReturn(TestData.USERNAME);
        }
    }

    private static class TestTarget {

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public String test() {
            return TestData.RESULT;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public String test(@Audited.Id long id) {
            return TestData.RESULT;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public String test(@Audited.Id("myIdParam") TestIdParameter parameter) {
            return TestData.RESULT;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public String test(@Audited.Detail TestDetailParameter parameter) {
            return TestData.RESULT;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public String test(@Audited.Detail(TestData.ARG1) TestDetailParameter parameter1,
                           @Audited.Detail(TestData.ARG2) TestDetailParameter parameter2) {

            return TestData.RESULT;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public @Audited.Id String testIdReturned() {
            return TestData.ID;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public @Audited.Id("myIdParam") TestIdParameter testIdFromReturnedObject() {
            return new TestIdParameter();
        }

        @Audited(subType = TestData.SUB_TYPE)
        public String testTypeNotSet() {
            return TestData.RESULT;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public String testUnknownIdField(@Audited.Id("myIdParam") Object parameter) {
            return TestData.RESULT;
        }

        @Audited(type = TestData.TYPE, subType = TestData.SUB_TYPE)
        public String testMultipleDetailsNoNames(@Audited.Detail TestDetailParameter parameter1,
                                                 @Audited.Detail TestDetailParameter parameter2) {

            return TestData.RESULT;
        }
    }

    private static class TestIdParameter {
        Long myIdParam;

        TestIdParameter() {
            this(Long.valueOf(TestData.ID));
        }

        TestIdParameter(Long myIdParam) {
            this.myIdParam = myIdParam;
        }
    }

    private static class TestDetailParameter {
        String field1;
        String field2;

        TestDetailParameter(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }
}
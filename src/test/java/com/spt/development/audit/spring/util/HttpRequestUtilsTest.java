package com.spt.development.audit.spring.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

class HttpRequestUtilsTest {
    private interface TestData {
        String IP_ADDRESS = "127.1.1.127";
        String REMOTE_ADDRESS = "271.1.1.271";
    }

    @Test
    void getClientIpAddress_noRequestAttributesInRequestContext_shouldReturnNull() {
        RequestContextHolder.setRequestAttributes(null);

        final String result = HttpRequestUtils.getClientIpAddress();

        assertThat(result, is(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    })
    void getClientIpAddress_candidateHeaderWithIpAddress_shouldReturnIpAddress(String headerWithIpAddress) {
        setRequestAttributes(headerWithIpAddress);

        final String result = HttpRequestUtils.getClientIpAddress();

        assertThat(result, is(TestData.IP_ADDRESS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    })
    void getClientIpAddress_candidateHeaderWithEmptyIpAddress_shouldReturnRemoteAddress(String headerWithIpAddress) {
        setRequestAttributes(headerWithIpAddress, StringUtils.EMPTY);

        final String result = HttpRequestUtils.getClientIpAddress();

        assertThat(result, is(TestData.REMOTE_ADDRESS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    })
    void getClientIpAddress_candidateHeaderWithUnknownIpAddress_shouldReturnRemoteAddress(String headerWithIpAddress) {
        setRequestAttributes(headerWithIpAddress, "unknown");

        final String result = HttpRequestUtils.getClientIpAddress();

        assertThat(result, is(TestData.REMOTE_ADDRESS));
    }

    @Test
    void getClientIpAddress_noCandidateHeaderWithIpAddress_shouldReturnRemoteAddress() {
        setRequestAttributes("non-candidate-header");

        final String result = HttpRequestUtils.getClientIpAddress();

        assertThat(result, is(TestData.REMOTE_ADDRESS));
    }

    private void setRequestAttributes(String headerWithIpAddress) {
        setRequestAttributes(headerWithIpAddress, TestData.IP_ADDRESS+ ",ignore-this");
    }

    private void setRequestAttributes(String headerWithIpAddress, String ipAddress) {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        if (headerWithIpAddress != null) {
            when(request.getHeader(headerWithIpAddress)).thenReturn(ipAddress);
        }
        when(request.getRemoteAddr()).thenReturn(TestData.REMOTE_ADDRESS);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
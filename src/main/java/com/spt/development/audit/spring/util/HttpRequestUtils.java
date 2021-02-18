package com.spt.development.audit.spring.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility methods for retrieving details from the request associated with the current thread, if one exists.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpRequestUtils {
    private static final String[] IP_HEADER_CANDIDATES = {
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
    };

    /**
     * Determines the client IP address from the current request.
     *
     * @return the client IP address or <code>null</code> if there is no request associated with the current thread.
     */
    public static String getClientIpAddress() {
        final RequestAttributes requestAttribute = RequestContextHolder.getRequestAttributes();

        if (requestAttribute == null) {
            return null;
        }
        return getClientIpAddress(((ServletRequestAttributes) requestAttribute));
    }

    private static String getClientIpAddress(ServletRequestAttributes requestAttributes) {
        final HttpServletRequest request = requestAttributes.getRequest();

        for (String header: IP_HEADER_CANDIDATES) {
            final String ipList = request.getHeader(header);

            if (StringUtils.isNotEmpty(ipList) && !"unknown".equalsIgnoreCase(ipList)) {
                return ipList.split(",")[0];
            }
        }
        return request.getRemoteAddr();
    }
}

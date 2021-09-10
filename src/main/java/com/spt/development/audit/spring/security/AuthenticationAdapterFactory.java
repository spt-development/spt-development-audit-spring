package com.spt.development.audit.spring.security;

/**
 * Factory used for creating {@link AuthenticationAdapter}s.
 */
public interface AuthenticationAdapterFactory {

    /**
     * Creates a new {@link AuthenticationAdapter} for the currently authenticated user.
     *
     * @return a new {@link AuthenticationAdapter} instance.
     */
    AuthenticationAdapter createAdapter();
}

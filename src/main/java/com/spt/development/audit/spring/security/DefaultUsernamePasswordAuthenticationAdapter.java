package com.spt.development.audit.spring.security;

import lombok.AllArgsConstructor;

/**
 * An {@link AuthenticationAdapter} used when the current user has been authenticated with a username/password. See
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}.
 */
@AllArgsConstructor
public class DefaultUsernamePasswordAuthenticationAdapter implements AuthenticationAdapter {
    private final Object principal;

    /**
     * Gets the result of calling {@link Object#toString()} on the principal representing the currently logged in user,
     * which is expected to be the username of the current user.
     *
     * @return the username of the current user.
     */
    @Override
    public String getUsername() {
        return principal.toString();
    }
}

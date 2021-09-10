package com.spt.development.audit.spring.security;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Supplier;

/**
 * An {@link AuthenticationAdapter} used when the current user has been authenticated with a username/password. See
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}.
 */
@AllArgsConstructor
public class DefaultUsernamePasswordAuthenticationAdapter implements AuthenticationAdapter {
    private final Supplier<String> usernameExtractor;

    /**
     * Creates a new instance of the authentication adapter.
     *
     * @param principal the principal to adapt.
     */
    public DefaultUsernamePasswordAuthenticationAdapter(final Object principal) {
        this.usernameExtractor = principal instanceof UserDetails ?
                () -> ((UserDetails)principal).getUsername() :
                () -> principal.toString();
    }

    /**
     * Gets the result of calling {@link UserDetails#getUsername()} if the principal representing the currently logged-
     * in user is {@link UserDetails}, otherwise falls back to calling {@link Object#toString()}.
     *
     * @return the username of the current user.
     */
    @Override
    public String getUsername() {
        return usernameExtractor.get();
    }
}

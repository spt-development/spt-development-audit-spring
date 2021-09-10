package com.spt.development.audit.spring.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Function;

/**
 * Creates an instance of {@link AuthenticationAdapter} from the `{@link Authentication} instance for the currently
 * logged in user retrieved by calling {@link SecurityContextHolder#getContext()}. Supports the following types of
 * authentication:
 * <ul>
 *     <li>Anonymous authentication - see {@link AnonymousAuthenticationToken}.</li>
 *     <li>Username / password authentication - see {@link UsernamePasswordAuthenticationToken}.</li>
 * </ul>
 */
public class DefaultAuthenticationAdapterFactory implements AuthenticationAdapterFactory {
    private Function<Authentication, AuthenticationAdapter> usernamePasswordFactory;

    /**
     * Creates a new instance of the factory.
     */
    public DefaultAuthenticationAdapterFactory() {
        this.usernamePasswordFactory = auth -> new DefaultUsernamePasswordAuthenticationAdapter(auth.getPrincipal());
    }

    /**
     * Extension point to override the {@link AuthenticationAdapter} that is instantiated when the current user has been
     * authenticated with a username/password, for example to be able to extract a user ID from the user principal which
     * is not supported by {@link DefaultUsernamePasswordAuthenticationAdapter} which will be returned by default.
     *
     * @param usernamePasswordFactory a {@link Function} used as a factory for instantiating the custom
     *                                {@link AuthenticationAdapter}.
     *
     * @return <code>this</code> to provide a fluent interface.
     */
    public AuthenticationAdapterFactory withUsernamePasswordFactory(
            Function<Authentication, AuthenticationAdapter> usernamePasswordFactory) {

        this.usernamePasswordFactory = usernamePasswordFactory;
        return this;
    }

    /**
     * Creates a new {@link AuthenticationAdapter} for the currently authenticated user.
     *
     * @return a new {@link AuthenticationAdapter} instance.
     */
    public AuthenticationAdapter createAdapter() {
        return createAdapter(SecurityContextHolder.getContext().getAuthentication());
    }

    private AuthenticationAdapter createAdapter(Authentication authentication) {
        if (isAnonymousAuthentication(authentication)) {
            return new AnonymousAuthenticationAdapter();
        }

        if (isUsernamePasswordAuthentication(authentication)) {
            return usernamePasswordFactory.apply(authentication);
        }
        throw new UnsupportedOperationException(
                "Only anonymous users or users authenticated via simple username/password authentication are currently supported"
        );
    }

    private boolean isAnonymousAuthentication(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    private boolean isUsernamePasswordAuthentication(Authentication authentication) {
        return authentication instanceof UsernamePasswordAuthenticationToken;
    }
}

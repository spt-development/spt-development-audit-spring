package com.spt.development.audit.spring.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.function.Function;

/**
 * Creates an instance of {@link AuthenticationAdapter} from the `{@link Authentication} instance for the currently
 * logged in user retrieved by calling {@link SecurityContextHolder#getContext()}. Supports the following types of
 * authentication:
 * <ul>
 *     <li>Anonymous authentication - see {@link AnonymousAuthenticationToken}.</li>
 *     <li>OAuth2 authentication - see {@link OAuth2Authentication}.</li>
 *     <li>Username / password authentication - see {@link UsernamePasswordAuthenticationToken}.</li>
 * </ul>
 */
public class AuthenticationAdapterFactory {
    private Function<Authentication, AuthenticationAdapter> usernamePasswordFactory;
    private Function<Authentication, AuthenticationAdapter> oauth2Factory;

    /**
     * Creates a new instance of the factory.
     */
    public AuthenticationAdapterFactory() {
        this.usernamePasswordFactory = auth -> new DefaultUsernamePasswordAuthenticationAdapter(auth.getPrincipal());
        this.oauth2Factory = auth -> new OAuth2AuthenticationAdapter(auth);
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
     * Extension point to override the {@link AuthenticationAdapter} that is instantiated when the current user has been
     * authenticated via OAuth2, for example to be able extract the user ID from the OAuth token which is not supported
     * by {@link OAuth2AuthenticationAdapter} which will be returned by default.
     *
     * @param oauth2Factory a {@link Function} used as a factory for instantiating the custom {@link AuthenticationAdapter}.
     *
     * @return <code>this</code> to provide a fluent interface.
     */
    public AuthenticationAdapterFactory withOauth2Factory(Function<Authentication, AuthenticationAdapter> oauth2Factory) {
        this.oauth2Factory = oauth2Factory;
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

        if (isOAuth2Authentication(authentication)) {
            return oauth2Factory.apply(authentication);
        }
        throw new UnsupportedOperationException(
                "Only anonymous users, users authenticated via OAuth2 or simple username/password authentication are currently supported"
        );
    }

    private boolean isAnonymousAuthentication(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    private boolean isUsernamePasswordAuthentication(Authentication authentication) {
        return authentication instanceof UsernamePasswordAuthenticationToken;
    }

    private boolean isOAuth2Authentication(Authentication authentication) {
        try {
            return authentication instanceof OAuth2Authentication;
        }
        catch (Exception ex) {
            if (ex instanceof ClassNotFoundException) {
                // If The OAuth2 class is not on the class path, then it *can't* be OAuth2 authentication - this is
                // possible becayse spring-security-oauth2 is marked as an optional dependency in the pom.
                return false;
            }
            throw ex;
        }
    }
}

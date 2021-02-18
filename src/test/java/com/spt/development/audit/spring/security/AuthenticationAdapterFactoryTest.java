package com.spt.development.audit.spring.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class AuthenticationAdapterFactoryTest {

    @Test
    void createAdapter_nullAuthentication_shouldReturnAnonymousAuthenticationAdapter() {
        setUpSecurityContext(null);

        final AuthenticationAdapter result = createFactory().createAdapter();

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(AnonymousAuthenticationAdapter.class));
    }

    @Test
    void createAdapter_anonymousAuthentication_shouldReturnAnonymousAuthenticationAdapter() {
        setUpSecurityContext(Mockito.mock(AnonymousAuthenticationToken.class));

        final AuthenticationAdapter result = createFactory().createAdapter();

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(AnonymousAuthenticationAdapter.class));
    }

    @Test
    void createAdapter_oAuth2Authentication_shouldReturnOAuth2AuthenticationAdapter() {
        setUpSecurityContext(Mockito.mock(OAuth2Authentication.class));

        final AuthenticationAdapter result = createFactory().createAdapter();

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(OAuth2AuthenticationAdapter.class));
    }

    @Test
    void createAdapter_oAuth2AuthenticationWithCustomFactory_shouldReturnCustomOAuth2AuthenticationAdapter() {
        final AuthenticationAdapter mockAuthAdapter = Mockito.mock(AuthenticationAdapter.class);

        setUpSecurityContext(Mockito.mock(OAuth2Authentication.class));

        final AuthenticationAdapter result = createFactory()
                .withOauth2Factory(a -> mockAuthAdapter)
                .createAdapter();

        assertThat(result, is(mockAuthAdapter));
    }

    @Test
    void createAdapter_usernamePasswordAuthentication_shouldReturnDefaultUsernamePasswordAuthenticationAdapter() {
        final AuthenticationAdapterFactory target = createFactory();
        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = Mockito.mock(UsernamePasswordAuthenticationToken.class);

        when(usernamePasswordAuthenticationToken.getPrincipal()).thenReturn("unhapp.path@test.com");

        setUpSecurityContext(usernamePasswordAuthenticationToken);

        final AuthenticationAdapter result = target.createAdapter();

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(DefaultUsernamePasswordAuthenticationAdapter.class));
    }

    @Test
    void createAdapter_usernamePasswordAuthenticationWithCustomFactory_shouldReturnCustomUsernamePasswordAuthenticationAdapter() {
        final AuthenticationAdapter mockAuthAdapter = Mockito.mock(AuthenticationAdapter.class);
        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = Mockito.mock(UsernamePasswordAuthenticationToken.class);

        when(usernamePasswordAuthenticationToken.getPrincipal()).thenReturn("unhapp.path@test.com");

        setUpSecurityContext(usernamePasswordAuthenticationToken);

        final AuthenticationAdapter result = createFactory()
                .withUsernamePasswordFactory(a -> mockAuthAdapter)
                .createAdapter();

        assertThat(result, is(mockAuthAdapter));
    }

    @Test
    void createAdapter_unsupportedAuthenticationMechanism_shouldThrowException() {
        final AuthenticationAdapterFactory target = createFactory();

        setUpSecurityContext(Mockito.mock(Authentication.class));

        final UnsupportedOperationException result = assertThrows(
                UnsupportedOperationException.class, target::createAdapter
        );

        assertThat(result, is(notNullValue()));
        assertThat(result.getMessage(), is("Only anonymous users, users authenticated via OAuth2 or simple username/password authentication are currently supported"));
    }

    private void setUpSecurityContext(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private AuthenticationAdapterFactory createFactory() {
        return new AuthenticationAdapterFactory();
    }
}
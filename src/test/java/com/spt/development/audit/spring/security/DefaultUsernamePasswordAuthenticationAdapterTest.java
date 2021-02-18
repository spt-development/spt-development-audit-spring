package com.spt.development.audit.spring.security;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class DefaultUsernamePasswordAuthenticationAdapterTest {
    private interface TestData {
        String PRINCIPAL = "Test principle";
    }

    @Test
    void getUserId_happyPath_shouldReturnNull() {
        final String result = createAdapter().getUserId();

        assertThat(result, is(nullValue()));
    }

    @Test
    void getUsername_happyPath_shouldReturnPrincipleString() {
        final String result = createAdapter().getUsername();

        assertThat(result, is(TestData.PRINCIPAL));
    }

    @Test
    void getUserAttribute_anyAttribute_shouldReturnNull() {
        final String result = createAdapter().getUserAttribute("change_password");

        assertThat(result, is(nullValue()));
    }

    private DefaultUsernamePasswordAuthenticationAdapter createAdapter() {
        return new DefaultUsernamePasswordAuthenticationAdapter(TestData.PRINCIPAL);
    }
}
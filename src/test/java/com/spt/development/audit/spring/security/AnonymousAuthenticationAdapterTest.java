package com.spt.development.audit.spring.security;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class AnonymousAuthenticationAdapterTest {

    @Test
    void getUserId_happyPath_shouldReturnNull() {
        final String result = createAdapter().getUserId();

        assertThat(result, is(nullValue()));
    }

    @Test
    void getUsername_happyPath_shouldReturnNull() {
        final String result = createAdapter().getUsername();

        assertThat(result, is(nullValue()));
    }

    @Test
    void getUserAttribute_anyAttribute_shouldReturnNull() {
        final String result = createAdapter().getUserAttribute("change_password");

        assertThat(result, is(nullValue()));
    }

    private AnonymousAuthenticationAdapter createAdapter() {
        return new AnonymousAuthenticationAdapter();
    }
}
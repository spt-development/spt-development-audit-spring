package com.spt.development.audit.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

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
    void getUsername_userDetails_shouldReturnUserDetailsUsername() {
        final String result = createAdapter(new TestUserDetails()).getUsername();

        assertThat(result, is(TestData.PRINCIPAL));
    }

    @Test
    void getUserAttribute_anyAttribute_shouldReturnNull() {
        final String result = createAdapter().getUserAttribute("change_password");

        assertThat(result, is(nullValue()));
    }

    private DefaultUsernamePasswordAuthenticationAdapter createAdapter() {
        return createAdapter(TestData.PRINCIPAL);
    }

    private DefaultUsernamePasswordAuthenticationAdapter createAdapter(Object principal) {
        return new DefaultUsernamePasswordAuthenticationAdapter(principal);
    }

    private static class TestUserDetails implements UserDetails {
        static final long serialVersionUID = 1L;

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return TestData.PRINCIPAL;
        }

        @Override
        public boolean isAccountNonExpired() {
            return false;
        }

        @Override
        public boolean isAccountNonLocked() {
            return false;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }
}
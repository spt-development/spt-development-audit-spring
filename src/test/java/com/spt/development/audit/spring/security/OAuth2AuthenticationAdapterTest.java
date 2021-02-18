package com.spt.development.audit.spring.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class OAuth2AuthenticationAdapterTest {
    private static final Gson GSON = new GsonBuilder().create();

    private interface OAuthToken {
        String UID = "uid";
        String CHANGE_PASSWORD = "change_password";
    }

    private interface TestData {
        String USER_ID = "1345";
        String USERNAME = "oauth2@test.com";
        boolean CHANGE_PASSWORD = true;
    }

    @Test
    void getUserId_validOAuth2Authentication_shouldReturnNull() {
        final String result = createAdapter().getUserId();

        assertThat(result, is(nullValue()));
    }

    @Test
    void getUsername_validOAuth2Authentication_shouldReturnPrincipal() {
        final String result = createAdapter().getUsername();

        assertThat(result, is(TestData.USERNAME));
    }

    @Test
    void getUserAttribute_validOAuth2AuthenticationWithoutUserAuthDetails_shouldReturnAttributeFromToken() {
        final String result = createAdapter().getUserAttribute(OAuthToken.CHANGE_PASSWORD);

        assertThat(result, is(Boolean.toString(TestData.CHANGE_PASSWORD)));
    }

    @Test
    void getUserAttribute_validOAuth2AuthenticationWithUserAuthDetailsWithoutAttribute_shouldReturnAttributeFromToken() {
        final Authentication authentication = Mockito.mock(Authentication.class);
        final OAuth2AuthenticationAdapterArgs args = new OAuth2AuthenticationAdapterArgs(createJwt());

        when(args.authentication.getUserAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(new HashMap<>());

        final String result = createAdapter(args).getUserAttribute(OAuthToken.CHANGE_PASSWORD);

        assertThat(result, is(Boolean.toString(TestData.CHANGE_PASSWORD)));
    }

    @Test
    void getUserAttribute_validOAuth2AuthenticationWithUserAuthDetailsWithAttribute_shouldReturnAttributeValue() {
        final Authentication authentication = Mockito.mock(Authentication.class);
        final OAuth2AuthenticationAdapterArgs args = new OAuth2AuthenticationAdapterArgs(createJwt());

        when(args.authentication.getUserAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(MapUtils.putAll(new HashMap<>(), new Object[] {
                new DefaultMapEntry<>(OAuthToken.UID, TestData.USER_ID),
                new DefaultMapEntry<>(OAuthToken.CHANGE_PASSWORD, !TestData.CHANGE_PASSWORD)
        }));

        final String result = createAdapter(args).getUserAttribute(OAuthToken.CHANGE_PASSWORD);

        assertThat(result, is(Boolean.toString(!TestData.CHANGE_PASSWORD)));
    }

    @Test
    void isChangePassword_jwtWithoutAttribute_shouldReturnNull() {
        final String result = createAdapter(createJwt(new HashMap<>())).getUserAttribute(OAuthToken.CHANGE_PASSWORD);

        assertThat(result, is(nullValue()));
    }

    @Nested
    class CustomOAuth2AuthenticationAdapter {
        @Test
        void getUserId_validOAuth2AuthenticationWithoutUserAuthDetails_shouldReturnUserIdFromToken() {
            final String result = createAdapter().getUserId();

            assertThat(result, is(TestData.USER_ID));
        }

        @Test
        void getUserId_validOAuth2AuthenticationWithUserAuthDetailsWithoutUID_shouldReturnUserIdFromToken() {
            final Authentication authentication = Mockito.mock(Authentication.class);
            final OAuth2AuthenticationAdapterArgs args = new OAuth2AuthenticationAdapterArgs(createJwt());

            when(args.authentication.getUserAuthentication()).thenReturn(authentication);
            when(authentication.getDetails()).thenReturn(new HashMap<>());

            final String result = createAdapter(args).getUserId();

            assertThat(result, is(TestData.USER_ID));
        }

        @Test
        void getUserId_validOAuth2AuthenticationWithUserAuthDetailsWithUID_shouldReturnUserId() {
            final Authentication authentication = Mockito.mock(Authentication.class);
            final OAuth2AuthenticationAdapterArgs args = new OAuth2AuthenticationAdapterArgs(createJwt());

            when(args.authentication.getUserAuthentication()).thenReturn(authentication);
            when(authentication.getDetails()).thenReturn(MapUtils.putAll(new HashMap<>(), new Object[] {
                    new DefaultMapEntry<>(OAuthToken.UID, TestData.USER_ID)
            }));

            final String result = createAdapter(args).getUserId();

            assertThat(result, is(TestData.USER_ID));
        }

        @Test
        void getUserId_accessTokenWithoutAnyDelimiters_shouldThrowException() {
            final OAuth2AuthenticationAdapter target = createAdapter("no delimiters");

            final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, target::getUserId);

            assertThat(result, is(notNullValue()));
            assertThat(result.getMessage(), containsString("no delimiter found"));
        }

        @Test
        void getUserId_accessTokenWithOnlyOneDelimiter_shouldThrowException() {
            final OAuth2AuthenticationAdapter target = createAdapter("only.one delimiter");

            final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, target::getUserId);

            assertThat(result, is(notNullValue()));
            assertThat(result.getMessage(), containsString("only one delimiter found"));
        }

        @Test
        void getUserId_accessTokenWithNoPayload_shouldThrowException() {
            final OAuth2AuthenticationAdapter target = createAdapter("..");

            final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, target::getUserId);

            assertThat(result, is(notNullValue()));
            assertThat(result.getMessage(), containsString("empty payload"));
        }

        @Test
        void getUserId_jwtWithoutUid_shouldThrowException() {
            final OAuth2AuthenticationAdapter target = createAdapter(createJwt(new HashMap<>()));

            final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, target::getUserId);

            assertThat(result, is(notNullValue()));
            assertThat(result.getMessage(), is("OAuth token does not contain attribute: 'uid'"));
        }

        private OAuth2AuthenticationAdapter createAdapter() {
            return createAdapter(createJwt());
        }

        private OAuth2AuthenticationAdapter createAdapter(String jwt) {
            return createAdapter(new OAuth2AuthenticationAdapterArgs(jwt));
        }

        private OAuth2AuthenticationAdapter createAdapter(OAuth2AuthenticationAdapterArgs args) {
            return new OAuth2AuthenticationAdapter(args.authentication) {
                @Override
                public String getUserId() {
                    return super.getUserAttribute(true, "uid");
                }
            };
        }
    }

    private OAuth2AuthenticationAdapter createAdapter() {
        return createAdapter(createJwt());
    }

    private String createJwt() {
        return createJwt(
                MapUtils.putAll(new HashMap<>(), new Object[] {
                        new DefaultMapEntry<>(OAuthToken.UID, TestData.USER_ID),
                        new DefaultMapEntry<>(OAuthToken.CHANGE_PASSWORD, TestData.CHANGE_PASSWORD)
                })
        );
    }

    private String createJwt(Map<String, String> payload) {
        final Map<String, String> header = MapUtils.putAll(new HashMap<>(), new Object[] {
                new DefaultMapEntry<>("alg", "HS256"),
                new DefaultMapEntry<>("typ", "JWT")
        });

        return String.format("%s.%s.%s",
                new String(Base64.getEncoder().encode(GSON.toJson(header).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8),
                new String(Base64.getEncoder().encode(GSON.toJson(payload).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8),
                UUID.randomUUID().toString()
        );
    }

    private OAuth2AuthenticationAdapter createAdapter(String jwt) {
        return createAdapter(new OAuth2AuthenticationAdapterArgs(jwt));
    }

    private OAuth2AuthenticationAdapter createAdapter(OAuth2AuthenticationAdapterArgs args) {
        return new OAuth2AuthenticationAdapter(args.authentication);
    }

    private static class OAuth2AuthenticationAdapterArgs {
        OAuth2Authentication authentication = Mockito.mock(OAuth2Authentication.class);

        OAuth2AuthenticationAdapterArgs(String jwt) {
            final OAuth2AuthenticationDetails details = Mockito.mock(OAuth2AuthenticationDetails.class);
            when(details.getTokenValue()).thenReturn(jwt);

            when(authentication.getDetails()).thenReturn(details);
            when(authentication.getPrincipal()).thenReturn(TestData.USERNAME);
        }
    }
}
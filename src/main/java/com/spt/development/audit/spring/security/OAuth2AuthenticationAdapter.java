package com.spt.development.audit.spring.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An {@link AuthenticationAdapter} used when the current user has been authenticated with OAuth2. See
 * {@link OAuth2Authentication}.
 */
public class OAuth2AuthenticationAdapter implements AuthenticationAdapter {
    private static final Gson GSON = new GsonBuilder().create();

    private static final char TOKEN_ELEMENT_DELIM = '.';

    private final OAuth2Authentication authentication;

    /**
     * Creates a new instance of {@link OAuth2AuthenticationAdapter} that wraps a {@link OAuth2Authentication} object.
     *
     * @param authentication an {@link Authentication} object that is expected to be of type {@link OAuth2Authentication}.
     */
    public OAuth2AuthenticationAdapter(Authentication authentication) {
        assert authentication instanceof OAuth2Authentication;

        this.authentication = (OAuth2Authentication) authentication;
    }

    /**
     * Parses the OAuth2 token an retrieves the token attribute identified by the key.
     *
     * @param key the key identifying the attribute to retrieve.
     *
     * @return the attribute from the OAuth2 token.
     */
    @Override
    public String getUserAttribute(String key) {
        return getUserAttribute(false, key);
    }

    /**
     * Gets an attribute from the OAuth token.
     *
     * @param required flag to determine whether the attribute is required or not. If <code>true</code> and the attribute
     *                 does not exists, an {@link IllegalArgumentException} is thrown.
     * @param key the attribute to retrieve from the token.
     *
     * @return the value of the token.
     */
    protected final String getUserAttribute(boolean required, String key) {
        if (authentication.getUserAuthentication() != null && authentication.getUserAuthentication().getDetails() instanceof Map) {
            final Map<String, ?> details = ((Map<?, ?>) authentication.getUserAuthentication().getDetails()).entrySet().stream()
                    .map(es -> Pair.of(es.getKey().toString(), es.getValue()))
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

            if (details.containsKey(key)) {
                return details.get(key).toString();
            }
        }
        return parseUserAttributeFromToken(required, key);
    }

    private String parseUserAttributeFromToken(boolean required, String key) {
        final String accessToken = OAuth2AuthenticationDetails.class.cast(authentication.getDetails()).getTokenValue();

        final int firstDelimIndex = accessToken.indexOf(TOKEN_ELEMENT_DELIM);

        if (firstDelimIndex < 0) {
            throw new IllegalArgumentException("OAuth token is not of expected format - no delimiter found");
        }

        final int lastDelimIndex = accessToken.lastIndexOf(TOKEN_ELEMENT_DELIM);

        if (lastDelimIndex == firstDelimIndex) {
            throw new IllegalArgumentException("OAuth token is not of expected format - only one delimiter found");
        }

        final String payload = accessToken.substring(firstDelimIndex + 1, lastDelimIndex);

        if (StringUtils.isEmpty(payload)) {
            throw new IllegalArgumentException("OAuth token has an empty payload");
        }

        final String payloadDecoded = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);

        final Map<String, Object> parsedPayload = GSON.fromJson(payloadDecoded, new MapStringObjectTypeToken().getType());

        if (!parsedPayload.containsKey(key) && required) {
            throw new IllegalArgumentException(String.format("OAuth token does not contain attribute: '%s'", key));
        }
        return Optional.ofNullable(parsedPayload.get(key)).map(Object::toString).orElse(null);
    }

    /**
     * Gets the result of calling {@link Object#toString()} on the principal returned by
     * {@link OAuth2Authentication#getPrincipal()}, which is expected to be the username of the current user.
     *
     * @return the username of the current user.
     */
    @Override
    public String getUsername() {
        return authentication.getPrincipal().toString();
    }

    private static class MapStringObjectTypeToken extends TypeToken<Map<String, Object>> {
    }
}

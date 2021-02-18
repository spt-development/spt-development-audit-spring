package com.spt.development.audit.spring.security;

/**
 * Adapter used to retrieve details about the currently authenticated user.
 */
public interface AuthenticationAdapter {

    /**
     * Gets the unique identifier for the current user.
     *
     * @return the user ID.
     */
    default String getUserId() {
        return null;
    }

    /**
     * Gets the username for the current user.
     *
     * @return the username.
     */
    default String getUsername() {
        return null;
    }

    /**
     * Gets an attribute of the current user, this will be implementation dependent.
     *
     * @param key the key identifying the attribute to retrieve.
     *
     * @return the value of the user attribute.
     */
    default String getUserAttribute(String key) {
        return null;
    }
}

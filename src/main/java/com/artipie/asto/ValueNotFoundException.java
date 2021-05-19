/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

/**
 * Exception indicating that value cannot be found in storage.
 *
 * @since 0.28
 */
@SuppressWarnings("serial")
public class ValueNotFoundException extends RuntimeException {

    /**
     * Ctor.
     *
     * @param key Key that was not found.
     */
    public ValueNotFoundException(final Key key) {
        super(message(key));
    }

    /**
     * Ctor.
     *
     * @param key Key that was not found.
     * @param cause Original cause for exception.
     */
    public ValueNotFoundException(final Key key, final Throwable cause) {
        super(message(key), cause);
    }

    /**
     * Build exception message for given key.
     *
     * @param key Key that was not found.
     * @return Message string.
     */
    private static String message(final Key key) {
        return String.format("No value for key: %s", key.string());
    }
}

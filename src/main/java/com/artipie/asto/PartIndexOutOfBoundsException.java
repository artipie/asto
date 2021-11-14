/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.ArtipieException;
import java.io.IOException;

/**
 * Exception indicating that part's index is out of bounds.
 *
 * @since 1.9.1
 */
@SuppressWarnings("serial")
public class PartIndexOutOfBoundsException extends ArtipieException {

    /**
     * Ctor.
     *
     * @param key Key
     * @param index Index of part in key
     */
    public PartIndexOutOfBoundsException(final Key key, final int index) {
        super(message(key, index));
    }

    /**
     * Ctor.
     *
     * @param key Key that was not found
     * @param index Index of part in key
     * @param cause Original cause for exception
     */
    public PartIndexOutOfBoundsException(final Key key, final int index, final Throwable cause) {
        super(new IOException(message(key, index), cause));
    }

    /**
     * Build exception message for given key and index.
     *
     * @param key Key that was not found
     * @param index Index of part in key
     * @return Message string.
     */
    private static String message(final Key key, final int index) {
        return String.format("No part for index %s in key %s", index, key.string());
    }
}

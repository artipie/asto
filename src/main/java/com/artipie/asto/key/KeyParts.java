/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.key;

import com.artipie.asto.Key;
import org.cactoos.list.ListEnvelope;
import org.cactoos.list.ListOf;

/**
 * Parts of a key.
 *
 * @since 1.8.1
 */
public final class KeyParts extends ListEnvelope<String> {

    /**
     * Delimiter used to split string into parts and join parts into string.
     */
    public static final String DELIMITER = "/";

    /**
     * Ctor.
     * @param key Key
     */
    public KeyParts(final Key key) {
        this(key.string());
    }

    /**
     * Ctor.
     * @param parts Parts in string
     */
    public KeyParts(final String parts) {
        this(parts.split(KeyParts.DELIMITER));
    }

    /**
     * Ctor.
     * @param parts Array of parts
     */
    public KeyParts(final String... parts) {
        super(new ListOf<>(parts));
    }
}

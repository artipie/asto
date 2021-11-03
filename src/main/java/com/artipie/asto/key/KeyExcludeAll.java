/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */

package com.artipie.asto.key;

import com.artipie.asto.Key;
import java.util.stream.Collectors;

/**
 * Key that excludes all occurrences of a part.
 *
 * @since 1.8.1
 */
public final class KeyExcludeAll extends Key.Wrap {

    /**
     * Ctor.
     * @param key Key
     * @param part Part to exclude
     */
    public KeyExcludeAll(final Key key, final String part) {
        super(
            new Key.From(
                new KeyParts(key).stream()
                    .filter(p -> !p.equals(part))
                    .collect(Collectors.toList())
            )
        );
    }
}

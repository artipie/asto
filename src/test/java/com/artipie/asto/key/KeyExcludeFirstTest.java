/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.key;

import com.artipie.asto.Key;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link KeyExcludeFirst}.
 *
 * @since 1.8.1
 */
@SuppressWarnings("PMD.TooManyMethods")
final class KeyExcludeFirstTest {

    @Test
    void excludesFirstPart() {
        final Key key = new Key.From("1", "2", "1");
        MatcherAssert.assertThat(
            new KeyExcludeFirst(key, "1").string(),
            new IsEqual<>("2")
        );
    }

    @Test
    void excludesNonExistingPart() {
        final Key key = new Key.From("1", "2");
        MatcherAssert.assertThat(
            new KeyExcludeFirst(key, "3").string(),
            new IsEqual<>("1/2")
        );
    }
}

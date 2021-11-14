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
 * Test case for {@link KeyExcludeLast}.
 *
 * @since 1.9.1
 */
final class KeyExcludeLastTest {

    @Test
    void excludesLastPart() {
        final Key key = new Key.From("1", "2", "1");
        MatcherAssert.assertThat(
            new KeyExcludeLast(key, "1").string(),
            new IsEqual<>("1/2")
        );
    }

    @Test
    void excludesNonExistingPart() {
        final Key key = new Key.From("3", "4");
        MatcherAssert.assertThat(
            new KeyExcludeLast(key, "5").string(),
            new IsEqual<>("3/4")
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.key;

import com.artipie.asto.Key;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link KeyParts}.
 *
 * @since 1.8.1
 */
@SuppressWarnings("PMD.TooManyMethods")
final class KeyPartsTest {

    @Test
    void getPartsFromKey() {
        final Key key = new Key.From("1", "2");
        MatcherAssert.assertThat(
            new KeyParts(key),
            Matchers.containsInRelativeOrder("1", "2")
        );
    }
}

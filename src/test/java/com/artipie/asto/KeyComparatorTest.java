/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link KeyComparator}.
 * @since 1.1.0
 */
final class KeyComparatorTest {
    @Test
    void comparesKeys() {
        final KeyComparator<Key> comparator = new KeyComparator<>();
        final Key frst = new Key.From("1");
        final Key scnd = new Key.From("2");
        MatcherAssert.assertThat(
            comparator.compare(frst, scnd),
            new IsEqual<>(-1)
        );
    }
}

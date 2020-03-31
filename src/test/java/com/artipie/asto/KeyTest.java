/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Key}.
 *
 * @since 1.0
 */
final class KeyTest {

    @Test
    void resolvesKeysFromParts() {
        MatcherAssert.assertThat(
            new Key.From("one1", "two2", "three3/four4").string(),
            new IsEqual<>("one1/two2/three3/four4")
        );
    }

    @Test
    void resolvesKeyFromParts() {
        MatcherAssert.assertThat(
            new Key.From("one", "two", "three").string(),
            Matchers.equalTo("one/two/three")
        );
    }

    @Test
    void resolvesKeyFromBasePath() {
        MatcherAssert.assertThat(
            new Key.From(new Key.From("black", "red"), "green", "yellow").string(),
            Matchers.equalTo("black/red/green/yellow")
        );
    }

    @Test
    void keyFromString() {
        final String string = "a/b/c";
        MatcherAssert.assertThat(
            new Key.From(string).string(),
            Matchers.equalTo(string)
        );
    }

    @Test
    void keyWithEmptyPart() {
        Assertions.assertThrows(Exception.class, () -> new Key.From("", "something").string());
    }

    @Test
    void resolvesRootKey() {
        MatcherAssert.assertThat(Key.ROOT.string(), Matchers.equalTo(""));
    }

    @Test
    void returnsParent() {
        MatcherAssert.assertThat(
            new Key.From("a/b").parent().get().string(),
            new IsEqual<>("a")
        );
    }

    @Test
    void rootParent() {
        MatcherAssert.assertThat(
            "ROOT parent is not empty",
            !Key.ROOT.parent().isPresent()
        );
    }

    @Test
    void emptyKeyParent() {
        MatcherAssert.assertThat(
            "Empty key parent is not empty",
            !new Key.From("").parent().isPresent()
        );
    }
}

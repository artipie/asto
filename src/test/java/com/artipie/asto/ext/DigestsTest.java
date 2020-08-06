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
package com.artipie.asto.ext;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link Digests}.
 * @since 0.24
 */
class DigestsTest {

    @ParameterizedTest
    @CsvSource({
        "MD5,MD5",
        "SHA1,SHA-1",
        "SHA256,SHA-256",
        "SHA512,SHA-512"
    })
    void providesCorrectMessageDigestAlgorithm(final Digests item, final String expected) {
        MatcherAssert.assertThat(
            item.get().getAlgorithm(),
            new IsEqual<>(expected)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "md5,MD5",
        "SHA-1,SHA1",
        "sha-256,SHA256",
        "SHa-512,SHA512"
    })
    void returnsCorrectDigestItem(final String from, final Digests item) {
        MatcherAssert.assertThat(
            new Digests.FromString(from).get(),
            new IsEqual<>(item)
        );
    }

    @Test
    void throwsExceptionOnUnknownAlgorithm() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Digests.FromString("123").get()
        );
    }

}

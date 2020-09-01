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
package com.artipie.asto.cache;

import com.artipie.asto.AsyncContent;
import com.artipie.asto.Key;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test case for {@link CacheControl}.
 *
 * @since 0.25
 */
final class CacheControlTest {

    static Object[][] verifyAllItemsParams() {
        return new Object[][]{
            new Object[]{CacheControl.Standard.ALWAYS, CacheControl.Standard.ALWAYS, true},
            new Object[]{CacheControl.Standard.ALWAYS, CacheControl.Standard.NO_CACHE, false},
            new Object[]{CacheControl.Standard.NO_CACHE, CacheControl.Standard.ALWAYS, false},
            new Object[]{CacheControl.Standard.NO_CACHE, CacheControl.Standard.NO_CACHE, false},
        };
    }

    @ParameterizedTest
    @MethodSource("verifyAllItemsParams")
    void verifyAllItems(final CacheControl first, final CacheControl second,
        final boolean expects) throws Exception {
        MatcherAssert.assertThat(
            new CacheControl.All(first, second)
                .validate(Key.ROOT, AsyncContent.EMPTY)
                .toCompletableFuture().get(),
            Matchers.is(expects)
        );
    }
}

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

import com.artipie.asto.Content;
import io.reactivex.Single;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ContentAs}.
 * @since 0.33
 */
class ContentAsTest {

    @Test
    void transformsToString() throws Exception {
        final String str = "abc012";
        MatcherAssert.assertThat(
            ContentAs.STRING.apply(Single.just(new Content.From(str.getBytes()))).toFuture().get(),
            new IsEqual<>(str)
        );
    }

    @Test
    void transformsToBytes() throws Exception {
        final byte[] bytes = "876hgf".getBytes();
        MatcherAssert.assertThat(
            ContentAs.BYTES.apply(Single.just(new Content.From(bytes))).toFuture().get(),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void transformsToLong() throws Exception {
        final long number = 12_087L;
        MatcherAssert.assertThat(
            ContentAs.LONG.apply(
                Single.just(new Content.From(String.valueOf(number).getBytes()))
            ).toFuture().get(),
            new IsEqual<>(number)
        );
    }

}

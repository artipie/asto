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
package com.artipie.asto.s3;

import com.artipie.asto.Content;
import hu.akarnokd.rxjava2.interop.MaybeInterop;
import io.reactivex.Flowable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ContentOfFuture}.
 *
 * @since 1.0
 */
final class ContentOfFutureTest {

    @Test
    void getsCompletedContent() {
        final AtomicInteger count = new AtomicInteger();
        final byte[] data = "xxx".getBytes(StandardCharsets.UTF_8);
        final Content content = new ContentOfFuture(
            CompletableFuture.<Content>supplyAsync(
                () -> new Content.From(data)
            ).whenComplete(
                (ignore, throwable) -> count.incrementAndGet()
            )
        );
        Assertions.assertArrayEquals(
            Flowable.fromPublisher(content)
                .singleElement()
                .to(MaybeInterop.get())
                .toCompletableFuture()
                .join()
                .array(),
            data
        );
        MatcherAssert.assertThat(
            content.size(), new IsEqual<>(Optional.of((long) data.length))
        );
        MatcherAssert.assertThat(
            count.get(), new IsEqual<>(1)
        );
    }
}

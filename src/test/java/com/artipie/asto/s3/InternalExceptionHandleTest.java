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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link InternalExceptionHandle}.
 *
 * @since 0.34
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class InternalExceptionHandleTest {

    @Test
    void translatesException() {
        final CompletableFuture<Void> future = CompletableFuture.runAsync(Assertions::fail);
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                ExecutionException.class,
                future.handle(
                    new InternalExceptionHandle<>(
                        AssertionError.class,
                        IllegalStateException::new
                    )
                )
                    .thenCompose(Function.identity())
                    .toCompletableFuture()
                    ::get
            ),
            Matchers.hasProperty("cause", Matchers.isA(IllegalStateException.class))
        );
    }

    @Test
    void failsAsIsWhenExceptionIsUnmatched() {
        final CompletableFuture<Void> future = CompletableFuture.runAsync(Assertions::fail);
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                ExecutionException.class,
                future.handle(
                    new InternalExceptionHandle<>(
                        NullPointerException.class,
                        IllegalStateException::new
                    )
                )
                    .thenCompose(Function.identity())
                    .toCompletableFuture()
                    ::get
            ),
            Matchers.hasProperty("cause", Matchers.isA(AssertionError.class))
        );
    }

    @Test
    void returnsValueIfNoErrorOccurs() throws ExecutionException, InterruptedException {
        final CompletableFuture<Object> future = CompletableFuture.supplyAsync(
            Object::new
        );
        MatcherAssert.assertThat(
            future
                .handle(
                    new InternalExceptionHandle<>(
                        AssertionError.class,
                        IllegalStateException::new
                    )
                )
                .thenCompose(Function.identity())
                .toCompletableFuture()
                .get(),
            Matchers.notNullValue()
        );
    }

}

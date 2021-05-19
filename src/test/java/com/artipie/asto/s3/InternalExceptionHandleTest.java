/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
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

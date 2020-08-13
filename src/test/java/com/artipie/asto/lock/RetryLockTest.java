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
package com.artipie.asto.lock;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.number.IsCloseTo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test cases for {@link RetryLock}.
 *
 * @since 0.24
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class RetryLockTest {

    /**
     * Scheduler used in tests.
     */
    private ScheduledExecutorService scheduler;

    @BeforeEach
    void setUp() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void tearDown() {
        this.scheduler.shutdown();
    }

    @Test
    @Timeout(3)
    void shouldSucceedAfterSomeAttempts() {
        final int attempts = 2;
        final AtomicInteger attempt = new AtomicInteger();
        new RetryLock(
            this.scheduler,
            new Lock() {
                @Override
                public CompletionStage<Void> acquire() {
                    final CompletableFuture<Void> result;
                    if (attempt.incrementAndGet() < attempts) {
                        result = new CompletableFuture<>();
                        result.completeExceptionally(new RuntimeException());
                    } else {
                        result = CompletableFuture.allOf();
                    }
                    return result;
                }

                @Override
                public CompletionStage<Void> release() {
                    throw new UnsupportedOperationException();
                }
            }
        ).acquire().toCompletableFuture().join();
        MatcherAssert.assertThat(
            attempt.get(),
            new IsEqual<>(attempts)
        );
    }

    @Test
    @Timeout(10)
    void shouldFailAfterMaxRetriesWithExtendingInterval() {
        final List<Long> attempts = new ArrayList<>(3);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final CompletionStage<Void> acquired = new RetryLock(
            this.scheduler,
            new Lock() {
                @Override
                public CompletionStage<Void> acquire() {
                    attempts.add(stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    final CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new RuntimeException());
                    return result;
                }

                @Override
                public CompletionStage<Void> release() {
                    throw new UnsupportedOperationException();
                }
            }
        ).acquire();
        Assertions.assertThrows(
            Exception.class,
            () -> acquired.toCompletableFuture().join(),
            "Fails to acquire"
        );
        MatcherAssert.assertThat(
            "Makes 3 attempts",
            attempts.size(), new IsEqual<>(3)
        );
        MatcherAssert.assertThat(
            "Makes 1st attempt almost instantly",
            attempts.get(0).doubleValue(),
            new IsCloseTo(0, 100)
        );
        MatcherAssert.assertThat(
            "Makes 2nd attempt in 500ms after 1st attempt",
            attempts.get(1).doubleValue() - attempts.get(0),
            new IsCloseTo(500, 100)
        );
        MatcherAssert.assertThat(
            "Makes 3rd attempt in 500ms * 1.5 = 750ms after 2nd",
            attempts.get(2).doubleValue() - attempts.get(1),
            new IsCloseTo(750, 100)
        );
    }
}

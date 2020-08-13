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

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.internal.InMemoryRetryRegistry;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Lock that tries to obtain origin {@link Lock} with retries.
 *
 * @since 0.24
 */
public final class RetryLock implements Lock {

    /**
     * Max number of attempts by default.
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Scheduler to use for retry triggering.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * Origin lock.
     */
    private final Lock origin;

    /**
     * Retry registry to store retries state.
     */
    private final InMemoryRetryRegistry registry;

    /**
     * Ctor.
     *
     * @param scheduler Scheduler to use for retry triggering.
     * @param origin Origin lock.
     */
    public RetryLock(final ScheduledExecutorService scheduler, final Lock origin) {
        this(
            scheduler,
            origin,
            new RetryConfig.Builder<>()
                .maxAttempts(RetryLock.MAX_ATTEMPTS)
                .intervalFunction(IntervalFunction.ofExponentialBackoff())
                .build()
        );
    }

    /**
     * Ctor.
     *
     * @param scheduler Scheduler to use for retry triggering.
     * @param origin Origin lock.
     * @param config Retry strategy.
     */
    public RetryLock(
        final ScheduledExecutorService scheduler,
        final Lock origin,
        final RetryConfig config
    ) {
        this.scheduler = scheduler;
        this.origin = origin;
        this.registry = new InMemoryRetryRegistry(config);
    }

    @Override
    public CompletionStage<Void> acquire() {
        return this.registry.retry("lock-acquire").executeCompletionStage(
            this.scheduler,
            this.origin::acquire
        );
    }

    @Override
    public CompletionStage<Void> release() {
        return this.registry.retry("lock-release").executeCompletionStage(
            this.scheduler,
            this.origin::release
        );
    }
}

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

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;

/**
 * Reactive adapter for {@link Lock}.
 *
 * @since 0.27
 */
public final class RxLock {

    /**
     * Origin.
     */
    private final Lock origin;

    /**
     * Ctor.
     *
     * @param origin Origin.
     */
    public RxLock(final Lock origin) {
        this.origin = origin;
    }

    /**
     * Acquire the lock.
     *
     * @return Completion of lock acquire operation.
     */
    public Completable acquire() {
        return Completable.defer(() -> CompletableInterop.fromFuture(this.origin.acquire()));
    }

    /**
     * Release the lock.
     *
     * @return Completion of lock release operation.
     */
    public Completable release() {
        return Completable.defer(() -> CompletableInterop.fromFuture(this.origin.release()));
    }
}

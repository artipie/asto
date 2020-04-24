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

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Single;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Storage synchronization.
 * @since 0.19
 * @checkstyle UnusedPrivateField (500 lines)
 * @checkstyle SingularField (500 lines)
 * @checkstyle NonStaticMethodCheck (500 lines)
 * @todo #160:30min Implement Sync class.
 *  This class currently is not implemented. This class should be implemented in a way to storage
 *  synchronization would work properly.
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField", "PMD.AvoidDuplicateLiterals"})
public class Sync {

    /**
     * The left storage.
     */
    private final Storage left;

    /**
     * The right storage.
     */
    private final Storage right;

    /**
     * Ctor.
     * @param left The left storage.
     * @param right The right storage.
     */
    public Sync(final Storage left, final Storage right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Synchronizes specified key from left storage with the right one.
     *
     * @param keys The keys.
     * @return When synchronization completes
     */
    public CompletableFuture<Void> leftWithRight(final List<Key> keys) {
        return Single.error(new IllegalStateException("not implemented")).ignoreElement()
            .to(CompletableInterop.await())
            .<Void>thenApply(o -> null)
            .toCompletableFuture();
    }

    /**
     * Synchronizes specified key from right storage with the left one.
     *
     * @param keys The keys.
     * @return When synchronization completes
     */
    public CompletableFuture<Void> rightWithLeft(final List<Key> keys) {
        return Single.error(new IllegalStateException("not implemented")).ignoreElement()
            .to(CompletableInterop.await())
            .<Void>thenApply(o -> null)
            .toCompletableFuture();
    }
}

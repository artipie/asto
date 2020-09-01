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

import com.artipie.asto.lock.Lock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Operation performed under lock.
 *
 * @param <T> Operation result type.
 * @since 0.27
 */
public final class UnderLockOperation<T> {

    /**
     * Lock.
     */
    private final Lock lock;

    /**
     * Operation.
     */
    private final Function<Storage, CompletionStage<T>> operation;

    /**
     * Ctor.
     *
     * @param lock Lock.
     * @param operation Operation.
     */
    public UnderLockOperation(
        final Lock lock,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        this.lock = lock;
        this.operation = operation;
    }

    /**
     * Perform operation under lock on storage.
     *
     * @param storage Storage.
     * @return Operation result.
     */
    public CompletionStage<T> perform(final Storage storage) {
        return this.lock.acquire().thenCompose(
            nothing -> this.operation.apply(storage).handle(
                (value, throwable) -> this.lock.release().thenCompose(
                    nthng -> {
                        final CompletableFuture<T> future = new CompletableFuture<>();
                        if (throwable == null) {
                            future.complete(value);
                        } else {
                            future.completeExceptionally(throwable);
                        }
                        return future;
                    }
                )
            ).thenCompose(Function.identity())
        );
    }
}

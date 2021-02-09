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

import com.artipie.asto.FailedCompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Translate an exception happened inside future.
 *
 * @param <T> Future result type.
 * @since 1.0
 */
final class InternalExceptionHandle<T> implements BiFunction<T, Throwable, CompletionStage<T>> {

    /**
     * The original future.
     */
    private final CompletableFuture<T> future;

    /**
     * Type of exception to handle.
     */
    private final Class<? extends Throwable> from;

    /**
     * Converter to a new exception.
     */
    private final Function<? super Throwable, ? extends Throwable> convert;

    /**
     * Ctor.
     *
     * @param future Original future.
     * @param from Internal type of exception.
     * @param convert Converter to a external type.
     */
    InternalExceptionHandle(
        final CompletableFuture<T> future,
        final Class<? extends Throwable> from,
        final Function<? super Throwable, ? extends Throwable> convert
    ) {
        this.future = future;
        this.from = from;
        this.convert = convert;
    }

    @Override
    public CompletionStage<T> apply(final T content, final Throwable throwable) {
        CompletionStage<T> result = this.future;
        if (
            throwable instanceof CompletionException
                && this.from.isInstance(throwable.getCause())
        ) {
            result = new FailedCompletionStage<>(
                this.convert.apply(throwable.getCause())
            );
        }
        return result;
    }

}

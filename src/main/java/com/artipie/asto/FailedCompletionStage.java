/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Completion stage that is failed when created.
 *
 * @since 0.30
 */
public class FailedCompletionStage<T> implements CompletionStage<T> {

    /**
     * Delegate completion stage.
     */
    private final CompletionStage<T> delegate;

    /**
     * Ctor.
     *
     * @param throwable Failure reason.
     */
    public FailedCompletionStage(final Throwable throwable) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        this.delegate = future;
    }

    @Override
    public <U> CompletionStage<U> thenApply(final Function<? super T, ? extends U> fn) {
        return this.delegate.thenApply(fn);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn) {
        return this.delegate.thenApplyAsync(fn);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn, final Executor executor) {
        return this.delegate.thenApplyAsync(fn, executor);
    }

    @Override
    public CompletionStage<Void> thenAccept(final Consumer<? super T> action) {
        return this.delegate.thenAccept(action);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action) {
        return this.delegate.thenAcceptAsync(action);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action, final Executor executor) {
        return this.delegate.thenAcceptAsync(action, executor);
    }

    @Override
    public CompletionStage<Void> thenRun(final Runnable action) {
        return this.delegate.thenRun(action);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(final Runnable action) {
        return this.delegate.thenRunAsync(action);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(final Runnable action, final Executor executor) {
        return this.delegate.thenRunAsync(action, executor);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombine(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn) {
        return this.delegate.thenCombine(other, fn);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn) {
        return this.delegate.thenCombineAsync(other, fn);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn, final Executor executor) {
        return this.delegate.thenCombineAsync(other, fn, executor);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action) {
        return this.delegate.thenAcceptBoth(other, action);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action) {
        return this.delegate.thenAcceptBothAsync(other, action);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action, final Executor executor) {
        return this.delegate.thenAcceptBothAsync(other, action, executor);
    }

    @Override
    public CompletionStage<Void> runAfterBoth(final CompletionStage<?> other, final Runnable action) {
        return this.delegate.runAfterBoth(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action) {
        return this.delegate.runAfterBothAsync(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action, final Executor executor) {
        return this.delegate.runAfterBothAsync(other, action, executor);
    }

    @Override
    public <U> CompletionStage<U> applyToEither(final CompletionStage<? extends T> other, final Function<? super T, U> fn) {
        return this.delegate.applyToEither(other, fn);
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(final CompletionStage<? extends T> other, final Function<? super T, U> fn) {
        return this.delegate.applyToEitherAsync(other, fn);
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(final CompletionStage<? extends T> other, final Function<? super T, U> fn, final Executor executor) {
        return this.delegate.applyToEitherAsync(other, fn, executor);
    }

    @Override
    public CompletionStage<Void> acceptEither(final CompletionStage<? extends T> other, final Consumer<? super T> action) {
        return this.delegate.acceptEither(other, action);
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(final CompletionStage<? extends T> other, final Consumer<? super T> action) {
        return this.delegate.acceptEitherAsync(other, action);
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(final CompletionStage<? extends T> other, final Consumer<? super T> action, final Executor executor) {
        return this.delegate.acceptEitherAsync(other, action, executor);
    }

    @Override
    public CompletionStage<Void> runAfterEither(final CompletionStage<?> other, final Runnable action) {
        return this.delegate.runAfterEither(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action) {
        return this.delegate.runAfterEitherAsync(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action, final Executor executor) {
        return this.delegate.runAfterEitherAsync(other, action, executor);
    }

    @Override
    public <U> CompletionStage<U> thenCompose(final Function<? super T, ? extends CompletionStage<U>> fn) {
        return this.delegate.thenCompose(fn);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn) {
        return this.delegate.thenComposeAsync(fn);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn, final Executor executor) {
        return this.delegate.thenComposeAsync(fn, executor);
    }

    @Override
    public <U> CompletionStage<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn) {
        return this.delegate.handle(fn);
    }

    @Override
    public <U> CompletionStage<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn) {
        return this.delegate.handleAsync(fn);
    }

    @Override
    public <U> CompletionStage<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn, final Executor executor) {
        return this.delegate.handleAsync(fn, executor);
    }

    @Override
    public CompletionStage<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action) {
        return this.delegate.whenComplete(action);
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action) {
        return this.delegate.whenCompleteAsync(action);
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action, final Executor executor) {
        return this.delegate.whenCompleteAsync(action, executor);
    }

    @Override
    public CompletionStage<T> exceptionally(final Function<Throwable, ? extends T> fn) {
        return this.delegate.exceptionally(fn);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this.delegate.toCompletableFuture();
    }
}

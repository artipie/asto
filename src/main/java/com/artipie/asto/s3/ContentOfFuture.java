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
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.reactivestreams.Subscriber;

/**
 * Content of a future.
 *
 * @since 1.0
 */
final class ContentOfFuture implements Content {
    /**
     * The future.
     */
    private final CompletionStage<Content> original;

    /**
     * Ctor.
     *
     * @param future The future to provide content.
     */
    ContentOfFuture(final CompletionStage<Content> future) {
        this.original = future;
    }

    @Override
    public Optional<Long> size() {
        return this.complete().size();
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        this.complete().subscribe(subscriber);
    }

    /**
     * Complete the future and handle checked exceptions.
     *
     * @return The content.
     */
    private Content complete() {
        try {
            return this.original.toCompletableFuture().get();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        } catch (final ExecutionException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

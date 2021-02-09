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
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.cqfn.rio.file.File;
import org.reactivestreams.Subscriber;

/**
 * Complements {@link Content} with size if size is unknown.
 * <p>
 * Size calculated by reading up to `limit` content bytes.
 * If end of content has not been reached by reading `limit` of bytes
 * then original content is returned.
 *
 * @since 1.0
 */
public final class EstimatedContent implements Content {
    /**
     * The original content.
     */
    private final Content content;

    /**
     * Ctor.
     *
     * @param original Original content.
     * @param limit Content reading limit.
     */
    public EstimatedContent(final Content original, final long limit) {
        this.content = new ContentOfFuture(
            EstimatedContent.initialize(original, limit)
        );
    }

    /**
     * Ctor.
     *
     * @param original Original content.
     */
    public EstimatedContent(final Content original) {
        this(original, Long.MAX_VALUE);
    }

    @Override
    public Optional<Long> size() {
        return this.content.size();
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        this.content.subscribe(subscriber);
    }

    /**
     * Initialize future of Content.
     *
     * @param original The original content.
     * @param limit The read limit.
     * @return The future.
     */
    private static CompletionStage<Content> initialize(
        final Content original,
        final long limit
    ) {
        final CompletableFuture<Content> res;
        if (original.size().isPresent()) {
            res = CompletableFuture.completedFuture(original);
        } else {
            res = EstimatedContent.readUntilLimit(original, limit);
        }
        return res;
    }

    /**
     * Read until limit.
     *
     * @param original The original content of unknown size.
     * @param limit The limit.
     * @return The future.
     */
    private static CompletableFuture<Content> readUntilLimit(
        final Content original,
        final long limit
    ) {
        final Path temp;
        try {
            temp = Files.createTempFile(
                S3Storage.class.getSimpleName(),
                ".upload.tmp"
            );
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new File(temp)
            .write(original)
            .thenCompose(
                nothing ->
                    Flowable.fromPublisher(
                        new File(temp).content()
                    )
                        .map(Buffer::remaining)
                        .scanWith(() -> 0L, Long::sum)
                        .takeUntil(total -> total >= limit)
                        .lastOrError()
                        .to(SingleInterop.get())
                        .toCompletableFuture()
            )
            .thenApply(
                last -> {
                    final Optional<Long> size;
                    if (last >= limit) {
                        size = Optional.empty();
                    } else {
                        size = Optional.of(last);
                    }
                    return size;
                }
            ).thenApply(
                sizeOpt -> {
                    final Flowable<ByteBuffer> data = Flowable.fromPublisher(
                        new File(temp).content()
                    ).doAfterTerminate(
                        () -> Files.delete(temp)
                    );
                    return sizeOpt
                        .<Content>map(size -> new From(size, data))
                        .orElse(new From(data));
                }
            )
            .handle(
                (value, throwable) -> {
                    final Content result;
                    if (throwable == null) {
                        result = value;
                    } else {
                        try {
                            Files.delete(temp);
                        } catch (final IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                        result = null;
                    }
                    return result;
                }
            )
            .toCompletableFuture();
    }
}

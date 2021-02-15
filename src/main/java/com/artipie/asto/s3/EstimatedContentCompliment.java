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

/**
 * Complements {@link Content} with size if size is unknown.
 * <p>
 * Size calculated by reading up to `limit` content bytes.
 * If end of content has not been reached by reading `limit` of bytes
 * then original content is returned.
 *
 * @since 0.34
 */
final class EstimatedContentCompliment {
    /**
     * The original content.
     */
    private final Content original;

    /**
     * The limit.
     */
    private final long limit;

    /**
     * Ctor.
     *
     * @param original Original content.
     * @param limit Content reading limit.
     */
    EstimatedContentCompliment(final Content original, final long limit) {
        this.original = original;
        this.limit = limit;
    }

    /**
     * Ctor.
     *
     * @param original Original content.
     */
    EstimatedContentCompliment(final Content original) {
        this(original, Long.MAX_VALUE);
    }

    /**
     * Initialize future of Content.
     *
     * @return The future.
     */
    public CompletionStage<Content> estimate() {
        final CompletableFuture<Content> res;
        if (this.original.size().isPresent()) {
            res = CompletableFuture.completedFuture(this.original);
        } else {
            res = this.readUntilLimit();
        }
        return res;
    }

    /**
     * Read until limit.
     *
     * @return The future.
     */
    private CompletableFuture<Content> readUntilLimit() {
        final Path temp;
        try {
            temp = Files.createTempFile(
                S3Storage.class.getSimpleName(),
                ".upload.tmp"
            );
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
        final Flowable<ByteBuffer> data = Flowable.fromPublisher(
            new File(temp).content()
        ).doOnError(error -> Files.deleteIfExists(temp));
        return new File(temp)
            .write(this.original)
            .thenCompose(
                nothing ->
                    data
                        .map(Buffer::remaining)
                        .scanWith(() -> 0L, Long::sum)
                        .takeUntil(total -> total >= this.limit)
                        .lastOrError()
                        .to(SingleInterop.get())
                        .toCompletableFuture()
            )
            .<Content>thenApply(
                last -> {
                    final Optional<Long> size;
                    if (last >= this.limit) {
                        size = Optional.empty();
                    } else {
                        size = Optional.of(last);
                    }
                    return new Content.From(
                        size,
                        data.doAfterTerminate(
                            () -> Files.deleteIfExists(temp)
                        )
                    );
                }
            ).toCompletableFuture();
    }
}

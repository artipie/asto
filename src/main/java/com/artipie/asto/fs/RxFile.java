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
package com.artipie.asto.fs;

import com.artipie.asto.Remaining;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * The reactive file allows you to perform read and write operations via {@link RxFile#flow()}
 * and {@link RxFile#save(Flowable)} methods respectively.
 * <p>
 * The implementation is based on Vert.x {@link io.vertx.reactivex.core.file.AsyncFile}.
 * <p>
 * This class must be constructed only via vertx instance, otherwise we can get file system from
 * another vertx instance and it will cause performance issues.
 * Check {@link io.vertx.core.Vertx original}  {@code fileSystem()} method docs.
 *
 * @since 0.12
 */
public class RxFile {

    /**
     * The file location of file system.
     */
    private final Path file;

    /**
     * The file system.
     */
    private final FileSystem fls;

    /**
     * Ctor.
     * @param file The wrapped file.
     * @param vertx Vertx instance.
     */
    public RxFile(final Path file, final Vertx vertx) {
        this.file = file;
        this.fls = vertx.fileSystem();
    }

    /**
     * Read file content as a flow of bytes.
     * @return A flow of bytes
     */
    public Flowable<ByteBuffer> flow() {
        return this.fls.rxOpen(
            this.file.toString(),
            new OpenOptions()
                .setRead(true)
                .setWrite(false)
                .setCreate(false)
        )
            .flatMapPublisher(
                asyncFile -> {
                    final Promise<Void> promise = Promise.promise();
                    final Completable completable = Completable.create(
                        emitter ->
                            promise.future().setHandler(
                                event -> {
                                    if (event.succeeded()) {
                                        emitter.onComplete();
                                    } else {
                                        emitter.onError(event.cause());
                                    }
                                }
                            )
                    );
                    return asyncFile.toFlowable().map(
                        buffer -> ByteBuffer.wrap(buffer.getBytes())
                    ).doOnTerminate(() -> asyncFile.rxClose().subscribe(promise::complete))
                        .mergeWith(completable);
                }
            );
    }

    /**
     * Save a flow of bytes to a file.
     *
     * @param flow The flow of bytes
     * @return Completion or error signal
     */
    public Completable save(final Flowable<ByteBuffer> flow) {
        return flow.reduce(
            (left, right) -> {
                final ByteBuffer concat = ByteBuffer.allocate(
                    left.remaining() + right.remaining()
                ).put(left).put(right);
                concat.flip();
                return concat;
            }
        )
            .map(buf -> new Remaining(buf).bytes())
            .toSingle(new byte[0])
            .flatMapCompletable(
                bytes -> Completable.fromAction(
                    () -> Files.write(
                        this.file,
                        bytes,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    )
                )
            );
    }

    /**
     * Move file to new location.
     *
     * @param target Target path the file is moved to.
     * @return Completion or error signal
     */
    public Completable move(final Path target) {
        return this.fls.rxMove(
            this.file.toString(),
            target.toString(),
            new CopyOptions().setReplaceExisting(true)
        );
    }

    /**
     * Delete file.
     *
     * @return Completion or error signal
     */
    public Completable delete() {
        return this.fls.rxDelete(this.file.toString());
    }

    /**
     * Get file size.
     *
     * @return File size in bytes.
     */
    public Single<Long> size() {
        return Single.fromCallable(() -> Files.size(this.file));
    }
}

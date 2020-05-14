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

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.CompletableSubject;
import io.reactivex.subjects.SingleSubject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import wtf.g4s8.rio.file.File;

/**
 * The reactive file allows you to perform read and write operations via {@link RxFile#flow()}
 * and {@link RxFile#save(Flowable)} methods respectively.
 * <p>
 * The implementation is based on Vert.x {@link io.vertx.reactivex.core.file.AsyncFile}.
 *
 * @since 0.12
 */
public class RxFile {

    /**
     * The file location of file system.
     */
    private final Path file;

    /**
     * IO executor.
     */
    private final ExecutorService exec;

    /**
     * Ctor.
     * @param file File path
     */
    public RxFile(final Path file) {
        this(file, Executors.newCachedThreadPool());
    }

    /**
     * Ctor.
     * @param file The wrapped file
     * @param exec IO executor
     */
    public RxFile(final Path file, final ExecutorService exec) {
        this.file = file;
        this.exec = exec;
    }

    /**
     * Read file content as a flow of bytes.
     * @return A flow of bytes
     */
    public Flowable<ByteBuffer> flow() {
        return Flowable.fromPublisher(new File(this.file).content(this.exec));
    }

    /**
     * Save a flow of bytes to a file.
     *
     * @param flow The flow of bytes
     * @return Completion or error signal
     */
    public Completable save(final Flowable<ByteBuffer> flow) {
        return CompletableInterop.fromFuture(
            new File(this.file).write(
                flow,
                this.exec,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
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
        final CompletableSubject res = CompletableSubject.create();
        this.exec.submit(
            () -> {
                try {
                    Files.move(this.file, target, StandardCopyOption.REPLACE_EXISTING);
                    res.onComplete();
                } catch (final IOException iex) {
                    res.onError(iex);
                }
            }
        );
        return res;
    }

    /**
     * Delete file.
     *
     * @return Completion or error signal
     */
    public Completable delete() {
        final CompletableSubject res = CompletableSubject.create();
        this.exec.submit(
            () -> {
                try {
                    Files.delete(this.file);
                    res.onComplete();
                } catch (final IOException iex) {
                    res.onError(iex);
                }
            }
        );
        return res;
    }

    /**
     * Get file size.
     *
     * @return File size in bytes.
     */
    public Single<Long> size() {
        final SingleSubject<Long> res = SingleSubject.create();
        this.exec.submit(
            () -> {
                try {
                    res.onSuccess(Files.size(this.file));
                } catch (final IOException iex) {
                    res.onError(iex);
                }
            }
        );
        return res;
    }
}

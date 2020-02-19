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

import com.artipie.asto.io.RxByteBuffers;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The reactive file allows you to perform read and write operations via {@link RxFile#flow()}
 * and {@link RxFile#save(Flowable)} methods respectively.
 * @since 0.12
 */
public class RxFile {

    /**
     * The file location of file system.
     */
    private final Path file;

    /**
     * RxIO operations.
     */
    private final RxByteBuffers channel;

    /**
     * Ctor.
     * @param file The wrapped file.
     * @param channel RxIO operations
     */
    public RxFile(final Path file, final RxByteBuffers channel) {
        this.file = file;
        this.channel = channel;
    }

    /**
     * A constructor with default args.
     * @param file The wrapped file.
     */
    public RxFile(final Path file) {
        this(file, new RxByteBuffers());
    }

    /**
     * Read file content as a flow of bytes.
     * @return A flow of bytes
     */
    public Flowable<ByteBuffer> flow() {
        return this.channel.path(this.file);
    }

    /**
     * Save a flow of bytes to a file.
     * @param flow The flow of bytes
     * @return Completion or error signal
     */
    public Completable save(final Flowable<ByteBuffer> flow) {
        return this.channel.path(flow, this.file)
            .to(Completable::fromPublisher);
    }

    /**
     * Move file to new location.
     * @param target Target path the file is moved to.
     * @return Completion or error signal
     */
    public Completable move(final Path target) {
        return Completable.fromAction(
            () -> Files.copy(this.file, target)
        );
    }
}

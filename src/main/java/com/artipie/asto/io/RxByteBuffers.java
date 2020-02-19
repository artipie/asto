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

package com.artipie.asto.io;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * A RxJava adapter for (N)IO operations with {@link ByteBuffer}.
 * On read buffers are read only as {@link ByteBuffer#asReadOnlyBuffer()}
 * @since 0.14
 */
public class RxByteBuffers {

    /**
     * Well known buffer size as a default size for ByteBuffers.
     * Package private for unit testing.
     */
    static final int BUFFER_SIZE = 8192;

    /**
     * Default options for read operations.
     */
    private static final Set<OpenOption> READS = Set.of(StandardOpenOption.READ);

    /**
     * Default options for write operations.
     */
    private static final Set<OpenOption> WRITES = Set.of(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    );

    /**
     * ByteBuffer factory.
     */
    private final Supplier<ByteBuffer> buffers;

    /**
     * A scheduler to observe on.
     */
    private final Scheduler scheduler;

    /**
     * Backpressure strategy for writing operations.
     */
    private final BackpressureStrategy backpressure;

    /**
     * All args constructor.
     * @param buffers ByteBuffer factory
     * @param scheduler Observe scheduler
     * @param backpressure Backpressure strategy
     */
    public RxByteBuffers(
        final Supplier<ByteBuffer> buffers,
        final Scheduler scheduler,
        final BackpressureStrategy backpressure
    ) {
        this.buffers = buffers;
        this.scheduler = scheduler;
        this.backpressure = backpressure;
    }

    /**
     * All default args constructor.
     */
    public RxByteBuffers() {
        this(
            () -> ByteBuffer.allocate(RxByteBuffers.BUFFER_SIZE),
            Schedulers.io(),
            BackpressureStrategy.BUFFER
        );
    }

    /**
     * Reads from a channel. Opens and closes it internally.
     * @param channels A readable channel factory
     * @return A Flowable of byte buffers
     */
    public Flowable<ByteBuffer> channel(final Callable<ReadableByteChannel> channels) {
        return Flowable.using(
            channels,
            channel -> Flowable.generate(
                new ByteBufferGenerator(channel, this.buffers.get())
            ),
            Channel::close
        ).observeOn(this.scheduler);
    }

    /**
     * Reads from a given file.
     * @param file A file to read from.
     * @param options Open options
     * @return A Flowable of byte buffers.
     */
    public Flowable<ByteBuffer> path(final Path file, final OpenOption... options) {
        return this.channel(
            () -> Files.newByteChannel(
                file, this.defaultOptions(options, RxByteBuffers.READS)
            )
        );
    }

    /**
     * Reads from a input stream. Opens and closes it internally.
     * @param streams An input stream factory
     * @return A Flowable of byte buffers.
     */
    public Flowable<ByteBuffer> stream(final Callable<InputStream> streams) {
        return this.channel(() -> Channels.newChannel(streams.call()));
    }

    /**
     * Rebufferizes given array.
     * @param array A byte array
     * @return A Flowable of byte buffers.
     */
    public Flowable<ByteBuffer> array(final byte[] array) {
        return this.stream(() -> new ByteArrayInputStream(array));
    }

    /**
     * Write to a channel. Opens and closes it internally.
     * @param bytes Readable bytes
     * @param channels A writable channel factory
     * @return A write-through Flowable of byte buffers
     */
    public Flowable<ByteBuffer> channel(
        final Flowable<ByteBuffer> bytes,
        final Callable<WritableByteChannel> channels
    ) {
        return Flowable.using(
            channels,
            channel -> Flowable.<ByteBuffer>create(
                emitter -> bytes.subscribe(new ByteBufferSubscriber(channel, emitter)),
                this.backpressure
            ),
            Channel::close
        ).observeOn(this.scheduler);
    }

    /**
     * Write to an OutputStream. Opens and closes it internally.
     * @param bytes Readable bytes
     * @param streams An OutputStream factory
     * @return A write-through Flowable of byte buffers
     */
    public Flowable<ByteBuffer> stream(
        final Flowable<ByteBuffer> bytes,
        final Callable<OutputStream> streams
    ) {
        return this.channel(bytes, () -> Channels.newChannel(streams.call()));
    }

    /**
     * Writes to a given file.
     * @param bytes Readable bytes
     * @param file A file to write to.
     * @param options Open options. If omitted, options are set to defaults.
     * @return A write-through Flowable of byte buffers.
     */
    public Flowable<ByteBuffer> path(
        final Flowable<ByteBuffer> bytes,
        final Path file,
        final OpenOption... options
    ) {
        return this.channel(
            bytes,
            () -> Files.newByteChannel(
                file,
                this.defaultOptions(options, RxByteBuffers.WRITES)
            )
        );
    }

    /**
     * Returns arg or defaults if the arg is empty.
     * @param arg Options by a method caller
     * @param defaults Default values
     * @return Arg or defaults if the arg is empty
     * @checkstyle NonStaticMethodCheck (5 lines)
     */
    private OpenOption[] defaultOptions(
        final OpenOption[] arg,
        final Collection<OpenOption> defaults
    ) {
        OpenOption[] options = arg;
        if (options == null || options.length == 0) {
            options = defaults.toArray(OpenOption[]::new);
        }
        return options;
    }
}

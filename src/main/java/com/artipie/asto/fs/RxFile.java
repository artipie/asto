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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The reactive file allows you to perform read and write operations via {@link RxFile#flow()}
 * and {@link RxFile#save(Flowable)} methods respectively.
 *
 * @since 0.12
 * @checkstyle NonStaticMethodCheck (500 lines)
 * @checkstyle AnonInnerLengthCheck (500 lines)
 */
public class RxFile {

    /**
     * The file location of file system.
     */
    private final Path file;

    /**
     * Ctor.
     * @param file The wrapped file.
     */
    public RxFile(final Path file) {
        this.file = file;
    }

    /**
     * Read file content as a flow of bytes.
     * @return A flow of bytes
     */
    public Flowable<Byte> flow() {
        return Flowable.fromPublisher(new RxFile.FromFilePublisher());
    }

    /**
     * Save a flow of bytes to a file.
     * @param flow The flow of bytes
     * @return Completion or error signal
     */
    public Completable save(final Flowable<Byte> flow) {
        return Completable.error(new IllegalStateException("Not implemented"));
    }

    /**
     * Publisher of file bytes.
     *
     * @since 0.12
     */
    class FromFilePublisher implements Publisher<Byte> {

        /**
         * The requested amount of bytes.
         */
        private Long requested = 0L;

        /**
         * The requested to read position in a file.
         */
        private Long rpos = 0L;

        /**
         * An actual amount of bytes which has been read.
         */
        private Long apos = 0L;

        /**
         * Is the stream is cancelled.
         */
        private Boolean cancelled = false;

        @Override
        public void subscribe(final Subscriber<? super Byte> subscriber) {
            final FromFilePublisher publisher = this;
            try {
                final AsynchronousFileChannel chan = AsynchronousFileChannel.open(
                    RxFile.this.file,
                    StandardOpenOption.READ
                );
                final long size = chan.size();
                subscriber.onSubscribe(
                    new Subscription() {
                        @Override
                        public void request(final long req) {
                            synchronized (publisher) {
                                if (!FromFilePublisher.this.cancelled) {
                                    FromFilePublisher.this.requested += req;
                                    final long remain = size - rpos;
                                    final long toread;
                                    if (FromFilePublisher.this.requested > remain) {
                                        toread = remain;
                                    } else {
                                        toread = FromFilePublisher.this.requested;
                                    }
                                    final ByteBuffer allocate = ByteBuffer.allocate((int) toread);
                                    chan.read(
                                        allocate,
                                        FromFilePublisher.this.rpos,
                                        allocate,
                                        new CompletionHandler<>() {
                                            @Override
                                            public void completed(final Integer read,
                                                final ByteBuffer res) {
                                                for (int idx = 0; idx < read; idx += 1) {
                                                    subscriber.onNext(res.get(idx));
                                                }
                                                synchronized (publisher) {
                                                    FromFilePublisher.this.apos += read;
                                                    if (FromFilePublisher.this.apos == size) {
                                                        subscriber.onComplete();
                                                    }
                                                }
                                            }

                                            @Override
                                            public void failed(final Throwable throwable,
                                                final ByteBuffer res) {
                                                subscriber.onError(throwable);
                                            }
                                        });
                                    FromFilePublisher.this.requested -= toread;
                                    FromFilePublisher.this.rpos += toread;
                                }
                            }
                        }

                        @Override
                        public void cancel() {
                            synchronized (publisher) {
                                FromFilePublisher.this.cancelled = true;
                            }
                        }
                    });
            } catch (final IOException exc) {
                subscriber.onError(exc);
            }
        }
    }
}

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

import io.reactivex.FlowableEmitter;
import io.reactivex.subscribers.DefaultSubscriber;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Creates a new write-through Flowable for further operations
 * to read an incoming Flowable just once.
 * Package-private as it should not be accessed directly.
 * @since 0.14
 */
class ByteBufferSubscriber extends DefaultSubscriber<ByteBuffer> {

    /**
     * A channel to write to.
     */
    private final WritableByteChannel channel;

    /**
     * A write through Flowable emitter.
     */
    private final FlowableEmitter<ByteBuffer> emitter;

    /**
     * All args constructor.
     * @param channel A channel to write to
     * @param emitter A write through Flowable emitter
     */
    ByteBufferSubscriber(
        final WritableByteChannel channel,
        final FlowableEmitter<ByteBuffer> emitter
    ) {
        this.channel = channel;
        this.emitter = emitter;
    }

    @Override
    public void onNext(final ByteBuffer buffer) {
        try {
            while (buffer.hasRemaining()) {
                this.channel.write(buffer);
            }
            this.emitter.onNext(buffer);
            this.request(1L);
        } catch (final IOException ex) {
            this.onError(ex);
        }
    }

    @Override
    public void onError(final Throwable throwable) {
        this.emitter.onError(throwable);
    }

    @Override
    public void onComplete() {
        this.emitter.onComplete();
    }

    @Override
    protected void onStart() {
        this.request(1L);
    }
}

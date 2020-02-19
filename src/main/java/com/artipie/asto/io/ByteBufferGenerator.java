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

import io.reactivex.Emitter;
import io.reactivex.functions.Consumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Reads buffers from a channels and then emits it as onNext signals as it goes.
 * Emits read only buffers.
 * Package-private as it should not be accessed directly.
 * @since 0.14
 */
class ByteBufferGenerator implements Consumer<Emitter<ByteBuffer>> {

    /**
     * A channel to read from.
     */
    private final ReadableByteChannel channel;

    /**
     * ByteBuffer.
     */
    private final ByteBuffer buffer;

    /**
     * All args constructor.
     * @param channel A channel to read from
     * @param buffer ByteBuffer
     */
    ByteBufferGenerator(
        final ReadableByteChannel channel,
        final ByteBuffer buffer
    ) {
        this.channel = channel;
        this.buffer = buffer;
    }

    @Override
    public void accept(final Emitter<ByteBuffer> emitter) throws Exception {
        try {
            if (this.channel.read(this.buffer.clear()) >= 0) {
                emitter.onNext(this.buffer.flip().asReadOnlyBuffer());
            } else {
                emitter.onComplete();
            }
        } catch (final IOException ex) {
            emitter.onError(ex);
        }
    }
}

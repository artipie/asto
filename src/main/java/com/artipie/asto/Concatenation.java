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
package com.artipie.asto;

import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;

/**
 * Concatenation of {@link ByteBuffer} instances.
 *
 * @since 0.17
 */
public class Concatenation {

    /**
     * Source of byte buffers.
     */
    private final Publisher<ByteBuffer> source;

    /**
     * Ctor.
     *
     * @param source Source of byte buffers.
     */
    public Concatenation(final Publisher<ByteBuffer> source) {
        this.source = source;
    }

    /**
     * Concatenates all buffers into single one.
     *
     * @return Single buffer.
     */
    public Single<ByteBuffer> single() {
        return Flowable.fromPublisher(this.source).reduce(
            ByteBuffer.allocate(0),
            (left, right) -> {
                left.mark();
                right.mark();
                final ByteBuffer concat = ByteBuffer.allocate(
                    left.remaining() + right.remaining()
                ).put(left).put(right);
                left.reset();
                right.reset();
                concat.flip();
                return concat;
            }
        );
    }
}

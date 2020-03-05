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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Content that can be stored in {@link Storage}.
 *
 * @since 0.15
 */
public interface Content extends Publisher<ByteBuffer> {

    /**
     * Provides size of content in bytes if known.
     *
     * @return Size of content in bytes if known.
     */
    Optional<Long> size();

    /**
     * Key built from byte buffers publisher and total size if it is known.
     *
     * @since 0.15
     */
    final class From implements Content {

        /**
         * Total content size in bytes, if known.
         */
        private final Optional<Long> length;

        /**
         * Content bytes.
         */
        private final Publisher<ByteBuffer> publisher;

        /**
         * Ctor.
         *
         * @param array Content bytes.
         */
        public From(final byte[] array) {
            this(
                array.length,
                Flowable.fromArray(ByteBuffer.wrap(Arrays.copyOf(array, array.length)))
            );
        }

        /**
         * Ctor.
         *
         * @param publisher Content bytes.
         */
        public From(final Publisher<ByteBuffer> publisher) {
            this(Optional.empty(), publisher);
        }

        /**
         * Ctor.
         *
         * @param size Total content size in bytes.
         * @param publisher Content bytes.
         */
        public From(final long size, final Publisher<ByteBuffer> publisher) {
            this(Optional.of(size), publisher);
        }

        /**
         * Ctor.
         *
         * @param size Total content size in bytes, if known.
         * @param publisher Content bytes.
         */
        public From(final Optional<Long> size, final Publisher<ByteBuffer> publisher) {
            this.length = size;
            this.publisher = publisher;
        }

        @Override
        public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
            this.publisher.subscribe(subscriber);
        }

        @Override
        public Optional<Long> size() {
            return this.length;
        }
    }
}

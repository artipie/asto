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

import java.nio.ByteBuffer;

/**
 * Remaining bytes in a byte buffer.
 * @since 0.13
 */
public final class Remaining {

    /**
     * The buffer.
     */
    private final ByteBuffer buf;

    /**
     * Restore buffer position.
     */
    private final boolean restore;

    /**
     * Ctor.
     * @param buf The byte buffer.
     */
    public Remaining(final ByteBuffer buf) {
        this(buf, false);
    }

    /**
     * Ctor.
     * @param buf The byte buffer.
     * @param restore Restore position.
     */
    public Remaining(final ByteBuffer buf, final boolean restore) {
        this.buf = buf;
        this.restore = restore;
    }

    /**
     * Obtain remaining bytes.
     * <p>
     * Read all remaining bytes from the buffer and reset position back after
     * reading.
     * </p>
     * @return Remaining bytes.
     */
    public byte[] bytes() {
        final byte[] bytes = new byte[this.buf.remaining()];
        if (this.restore) {
            this.buf.mark();
        }
        this.buf.get(bytes);
        if (this.restore) {
            this.buf.reset();
        }
        return bytes;
    }
}

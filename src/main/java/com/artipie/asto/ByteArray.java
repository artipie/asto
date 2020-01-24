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

import java.util.List;

/**
 * Byte array wrapper with ability to transform it to
 * boxed and primitive array.
 *
 * @since 0.7
 */
public class ByteArray {

    /**
     * Bytes.
     */
    private final Byte[] bytes;

    /**
     * Ctor for a list of byes.
     *
     * @param bytes The list of bytes
     */
    public ByteArray(final List<Byte> bytes) {
        this(fromList(bytes));
    }

    /**
     * Ctor for a primitive array.
     *
     * @param bytes The primitive bytes
     */
    public ByteArray(final byte[] bytes) {
        this(boxed(bytes));
    }

    /**
     * Ctor.
     *
     * @param bytes The bytes.
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public ByteArray(final Byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Return primitive byte array.
     *
     * @return Primitive byte array
     */
    public byte[] primitiveBytes() {
        final byte[] result = new byte[this.bytes.length];
        for (int itr = 0; itr < this.bytes.length; itr += 1) {
            result[itr] = this.bytes[itr];
        }
        return result;
    }

    /**
     * Return primitive byte array.
     *
     * @return Primitive byte array
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Byte[] boxedBytes() {
        return this.bytes;
    }

    /**
     * Convert primitive to boxed array.
     * @param primitive Primitive byte array
     * @return Boxed byte array
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static Byte[] boxed(final byte[] primitive) {
        final Byte[] res = new Byte[primitive.length];
        for (int itr = 0; itr < primitive.length; itr += 1) {
            res[itr] = primitive[itr];
        }
        return res;
    }

    /**
     * Convert list of bytes to byte array.
     * @param list The list of bytes.
     * @return Boxed byte array
     */
    private static Byte[] fromList(final List<Byte> list) {
        return list.toArray(new Byte[0]);
    }
}

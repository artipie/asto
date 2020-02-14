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
package com.artipie.asto.blocking;

import com.artipie.asto.ByteArray;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

/**
 * More primitive and easy to use wrapper to use {@code Storage}.
 *
 * @since 0.1
 */
public class BlockingStorage {

    /**
     * Wrapped storage.
     */
    private final RxStorage storage;

    /**
     * Wrap a {@link Storage} in order get a blocking version of it.
     *
     * @param storage Storage to wrap
     */
    public BlockingStorage(final Storage storage) {
        this.storage = new RxStorageWrapper(storage);
    }

    /**
     * This file exists?
     *
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise
     */
    public boolean exists(final Key key) {
        return this.storage.exists(key).blockingGet();
    }

    /**
     * Return the list of keys that start with this prefix, for
     * example "foo/bar/".
     *
     * @param prefix The prefix.
     * @return Collection of relative keys.
     */
    public Collection<Key> list(final Key prefix) {
        return this.storage.list(prefix).blockingGet();
    }

    /**
     * Save the content.
     *
     * @param key The key
     * @param content The content
     */
    public void save(final Key key, final byte[] content) {
        this.storage.save(
            key,
            Flowable.fromArray(ByteBuffer.wrap(content))
        ).blockingAwait();
    }

    /**
     * Moves value from one location to another.
     *
     * @param source Source key.
     * @param destination Destination key.
     */
    public void move(final Key source, final Key destination) {
        this.storage.move(source, destination).blockingAwait();
    }

    /**
     * Obtain value for the specified key.
     *
     * @param key The key
     * @return Value associated with the key
     */
    public byte[] value(final Key key) {
        return new ByteArray(
            this.storage.value(key)
                .blockingGet()
                .toList()
                .blockingGet()
                .stream()
                .map(buf -> new Remaining(buf).bytes())
                .flatMap(byteArr -> Arrays.stream(new ByteArray(byteArr).boxedBytes()))
                .toArray(Byte[]::new)
        ).primitiveBytes();
    }
}

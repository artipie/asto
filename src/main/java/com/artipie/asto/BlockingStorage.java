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

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Collection;
import org.reactivestreams.FlowAdapters;

/**
 * More primitive and easy to use wrapper to use {@code Storage}.
 *
 * @since 0.1
 */
public class BlockingStorage {

    /**
     * Wrapped storage.
     */
    private final Storage storage;

    /**
     * Wrap a {@link Storage} in order get a blocking version of it.
     *
     * @param storage Storage to wrap
     */
    public BlockingStorage(final Storage storage) {
        this.storage = storage;
    }

    /**
     * This file exists?
     *
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise
     */
    public boolean exists(final String key) {
        return Single.fromFuture(this.storage.exists(key)).blockingGet();
    }

    /**
     * Return the list of object names that start with this prefix, for
     * example "foo/bar/".
     * <p>
     * The prefix must end with a slash.
     *
     * @param prefix The prefix, ended with a slash
     * @return List of object keys/names
     */
    public Collection<String> list(final String prefix) {
        return Single.fromFuture(this.storage.list(prefix)).blockingGet();
    }

    /**
     * Save the content.
     *
     * @param key The key
     * @param content The content
     */
    public void save(final String key, final byte[] content) {
        Single.fromFuture(
            this.storage.save(
                key,
                FlowAdapters.toFlowPublisher(
                    Flowable.fromArray(
                        new ByteArray(
                            content
                        ).boxedBytes()
                    )
                )
            )
        ).blockingGet();
    }

    /**
     * Obtain value for the specified key.
     *
     * @param key The key
     * @return Value associated with the key
     */
    public byte[] value(final String key) {
        return new ByteArray(
            Flowable.fromPublisher(
                FlowAdapters.toPublisher(
                    Single.fromFuture(
                        this.storage.value(key)
                    ).blockingGet()
                )
            ).toList().blockingGet().toArray(new Byte[0])
        ).primitiveBytes();
    }
}

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
package com.artipie.asto.rx;

import com.artipie.asto.Key;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

/**
 * A reactive version of {@link com.artipie.asto.Storage}.
 *
 * @since 0.10
 */
public interface RxStorage {

    /**
     * This file exists?
     *
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise
     */
    Single<Boolean> exists(Key key);

    /**
     * Return the list of keys that start with this prefix, for
     * example "foo/bar/".
     *
     * @param prefix The prefix.
     * @return Collection of relative keys.
     */
    Single<Collection<Key>> list(Key prefix);

    /**
     * Saves the bytes to the specified key.
     *
     * @param key The key
     * @param content Bytes to save
     * @return Completion or error signal.
     */
    Completable save(Key key, Flowable<ByteBuffer> content);

    /**
     * Moves value from one location to another.
     *
     * @param source Source key.
     * @param destination Destination key.
     * @return Completion or error signal.
     */
    Completable move(Key source, Key destination);

    /**
     * Obtain bytes by key.
     *
     * @param key The key
     * @return Bytes.
     */
    Single<Flowable<ByteBuffer>> value(Key key);

    /**
     * Start a transaction with specified keys.
     *
     * @param keys The keys regarding which transaction is atomic
     * @return Transaction
     */
    Single<RxTransaction> transaction(List<Key> keys);
}

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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

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
     * @throws InterruptedException If thread was interrupted
     */
    public boolean exists(final Key key) throws InterruptedException {
        try {
            return this.storage.exists(key).get();
        } catch (final ExecutionException err) {
            throw new UncheckedExecutionException(err);
        }
    }

    /**
     * Return the list of keys that start with this prefix, for
     * example "foo/bar/".
     *
     * @param prefix The prefix.
     * @return Collection of relative keys.
     * @throws InterruptedException If thread was interrupted
     */
    public Collection<Key> list(final Key prefix) throws InterruptedException {
        try {
            return this.storage.list(prefix).get();
        } catch (final ExecutionException err) {
            throw new UncheckedExecutionException(err);
        }
    }

    /**
     * Save the content.
     *
     * @param key The key
     * @param content The content
     * @throws InterruptedException If thread was interrupted
     */
    public void save(final Key key, final byte[] content) throws InterruptedException {
        try {
            this.storage.save(
                key,
                new Content.OneTime(new Content.From(content))
            ).get();
        } catch (final ExecutionException err) {
            throw new UncheckedExecutionException(err);
        }
    }

    /**
     * Moves value from one location to another.
     *
     * @param source Source key.
     * @param destination Destination key.
     * @throws InterruptedException If thread was interrupted
     */
    public void move(final Key source, final Key destination) throws InterruptedException {
        try {
            this.storage.move(source, destination).get();
        } catch (final ExecutionException err) {
            throw new UncheckedExecutionException(err);
        }
    }

    /**
     * Get value size.
     *
     * @param key The key of value.
     * @return Size of value in bytes.
     * @throws InterruptedException If thread was interrupted
     */
    public long size(final Key key) throws InterruptedException {
        try {
            return this.storage.size(key).get();
        } catch (final ExecutionException err) {
            throw new UncheckedExecutionException(err);
        }
    }

    /**
     * Obtain value for the specified key.
     *
     * @param key The key
     * @return Value associated with the key
     * @throws InterruptedException If thread was interrupted
     */
    public byte[] value(final Key key) throws InterruptedException {
        try {
            return new Remaining(
                this.storage.value(key).thenApplyAsync(
                    pub -> new Concatenation(new OneTimePublisher<>(pub)).single().blockingGet()
                ).get(),
                true
            ).bytes();
        } catch (final ExecutionException err) {
            throw new UncheckedExecutionException(err);
        }
    }

    /**
     * Removes value from storage. Fails if value does not exist.
     *
     * @param key Key for value to be deleted.
     * @throws InterruptedException If thread was interrupted
     */
    public void delete(final Key key) throws InterruptedException {
        try {
            this.storage.delete(key).get();
        } catch (final ExecutionException err) {
            throw new UncheckedExecutionException(err);
        }
    }
}

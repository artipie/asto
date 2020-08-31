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

import com.artipie.asto.fs.FileStorage;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * The storage.
 * <p>
 * You are supposed to implement this interface the way you want. It has
 * to abstract the binary storage. You may use {@link FileStorage} if you
 * want to work with files. Otherwise, for S3 or something else, you have
 * to implement it yourself.
 *
 * @since 0.1
 */
public interface Storage {

    /**
     * This file exists?
     *
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise
     */
    CompletableFuture<Boolean> exists(Key key);

    /**
     * Return the list of keys that start with this prefix, for
     * example "foo/bar/".
     *
     * @param prefix The prefix.
     * @return Collection of relative keys.
     */
    CompletableFuture<Collection<Key>> list(Key prefix);

    /**
     * Saves the bytes to the specified key.
     *
     * @param key The key
     * @param content Bytes to save
     * @return Completion or error signal.
     */
    CompletableFuture<Void> save(Key key, Content content);

    /**
     * Moves value from one location to another.
     *
     * @param source Source key.
     * @param destination Destination key.
     * @return Completion or error signal.
     */
    CompletableFuture<Void> move(Key source, Key destination);

    /**
     * Get value size.
     *
     * @param key The key of value.
     * @return Size of value in bytes.
     */
    CompletableFuture<Long> size(Key key);

    /**
     * Obtain bytes by key.
     *
     * @param key The key
     * @return Bytes.
     */
    CompletableFuture<Content> value(Key key);

    /**
     * Removes value from storage. Fails if value does not exist.
     *
     * @param key Key for value to be deleted.
     * @return Completion or error signal.
     */
    CompletableFuture<Void> delete(Key key);

    /**
     * Runs operation exclusively for specified key.
     *
     * @param key Key which is scope of operation.
     * @param operation Operation to be performed exclusively.
     * @param <T> Operation result type.
     * @return Result of operation.
     */
    <T> CompletionStage<T> exclusively(
        Key key,
        Function<Storage, CompletionStage<T>> operation
    );

    /**
     * Forwarding decorator for {@link Storage}.
     *
     * @since 0.18
     */
    abstract class Wrap implements Storage {

        /**
         * Delegate storage.
         */
        private final Storage delegate;

        /**
         * Ctor.
         *
         * @param delegate Delegate storage
         */
        protected Wrap(final Storage delegate) {
            this.delegate = delegate;
        }

        @Override
        public final CompletableFuture<Boolean> exists(final Key key) {
            return this.delegate.exists(key);
        }

        @Override
        public final CompletableFuture<Collection<Key>> list(final Key prefix) {
            return this.delegate.list(prefix);
        }

        @Override
        public final CompletableFuture<Void> save(final Key key, final Content content) {
            return this.delegate.save(key, content);
        }

        @Override
        public final CompletableFuture<Void> move(final Key source, final Key destination) {
            return this.delegate.move(source, destination);
        }

        @Override
        public final CompletableFuture<Long> size(final Key key) {
            return this.delegate.size(key);
        }

        @Override
        public final CompletableFuture<Content> value(final Key key) {
            return this.delegate.value(key);
        }

        @Override
        public final CompletableFuture<Void> delete(final Key key) {
            return this.delegate.delete(key);
        }

        @Override
        public final <T> CompletionStage<T> exclusively(
            final Key key,
            final Function<Storage, CompletionStage<T>> operation
        ) {
            return this.delegate.exclusively(key, operation);
        }
    }
}

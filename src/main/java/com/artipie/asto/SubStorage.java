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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sub storage is a storage in storage.
 * <p>
 * It decorates origin storage and proxies all calls by appending prefix key.
 * </p>
 * @since 0.21
 * @todo #139:30min Implement prefixed transaction support.
 *  Transaction extends storage, so it's needed to implement all transactions method
 *  with prefixed key too. Also, create unit tests for sub storage to verify that
 *  all methods uses prefixed keys.
 */
public final class SubStorage implements Storage {

    /**
     * Prefix.
     */
    private final Key prefix;

    /**
     * Origin storage.
     */
    private final Storage origin;

    /**
     * Sub storage with prefix.
     * @param prefix Prefix key
     * @param origin Origin key
     */
    public SubStorage(final Key prefix, final Storage origin) {
        this.prefix = prefix;
        this.origin = origin;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return this.origin.exists(new PrefixedKed(this.prefix, key));
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key filter) {
        final Pattern ptn = Pattern.compile(String.format("%s/", this.prefix.string()));
        return this.origin.list(new PrefixedKed(this.prefix, filter)).thenApply(
            keys -> keys.stream()
                .map(key -> new Key.From(ptn.matcher(key.string()).replaceFirst("")))
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.origin.save(new PrefixedKed(this.prefix, key), content);
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return this.origin.move(
            new PrefixedKed(this.prefix, source),
            new PrefixedKed(this.prefix, destination)
        );
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        return this.origin.size(new PrefixedKed(this.prefix, key));
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return this.origin.value(new PrefixedKed(this.prefix, key));
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.origin.delete(new PrefixedKed(this.prefix, key));
    }

    @Override
    public CompletionStage<Void> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<Void>> operation
    ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Key with prefix.
     * @since 0.21
     */
    public static final class PrefixedKed extends Key.Wrap {

        /**
         * Key with prefix.
         * @param prefix Prefix key
         * @param key Key
         */
        public PrefixedKed(final Key prefix, final Key key) {
            super(new Key.From(prefix, key));
        }
    }
}

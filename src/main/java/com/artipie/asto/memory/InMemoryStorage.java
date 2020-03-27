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
package com.artipie.asto.memory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.Transaction;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Simple implementation of Storage that holds all data in memory.
 *
 * @since 0.14
 */
public final class InMemoryStorage implements Storage {

    /**
     * Values stored by key strings.
     */
    private final NavigableMap<String, byte[]> data;

    /**
     * Ctor.
     */
    public InMemoryStorage() {
        this.data = new TreeMap<>();
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                synchronized (this.data) {
                    return this.data.containsKey(key.string());
                }
            }
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key root) {
        return CompletableFuture.supplyAsync(
            () -> {
                synchronized (this.data) {
                    final String prefix = root.string();
                    final Collection<Key> keys = new LinkedList<>();
                    for (final String string : this.data.navigableKeySet().tailSet(prefix)) {
                        if (string.startsWith(prefix)) {
                            keys.add(new Key.From(string));
                        } else {
                            break;
                        }
                    }
                    return keys;
                }
            }
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return CompletableFuture.runAsync(
            () -> {
                synchronized (this.data) {
                    this.data.put(
                        key.string(),
                        content.bytes().blockingGet()
                    );
                }
            }
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return CompletableFuture.runAsync(
            () -> {
                synchronized (this.data) {
                    final String key = source.string();
                    if (!this.data.containsKey(key)) {
                        throw new IllegalArgumentException(
                            String.format("No value for source key: %s", source.string())
                        );
                    }
                    this.data.put(destination.string(), this.data.get(key));
                    this.data.remove(source.string());
                }
            }
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                synchronized (this.data) {
                    final byte[] content = this.data.get(key.string());
                    if (content == null) {
                        throw new IllegalArgumentException(
                            String.format("No value for key: %s", key.string())
                        );
                    }
                    return new Content.From(content);
                }
            }
        );
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return CompletableFuture.runAsync(
            () -> {
                synchronized (this.data) {
                    final String str = key.string();
                    if (!this.data.containsKey(str)) {
                        throw new IllegalArgumentException(
                            String.format("Key does not exist: %s", str)
                        );
                    }
                    this.data.remove(str);
                }
            }
        );
    }

    @Override
    public CompletableFuture<Transaction> transaction(final List<Key> keys) {
        return CompletableFuture.completedFuture(new InMemoryTransaction(this));
    }
}

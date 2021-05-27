/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.UnderLockOperation;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.lock.storage.StorageLock;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Simple implementation of Storage that holds all data in memory.
 *
 * @since 0.14
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class InMemoryStorage implements Storage {

    /**
     * Values stored by key strings.
     * It is package private for avoid using sync methods for operations of storage for benchmarks.
     * @checkstyle VisibilityModifierCheck (2 lines)
     */
    final NavigableMap<String, byte[]> data;

    /**
     * Ctor.
     */
    public InMemoryStorage() {
        this(new TreeMap<>());
    }

    /**
     * Ctor.
     * @param data Content of storage
     */
    InMemoryStorage(final NavigableMap<String, byte[]> data) {
        this.data = data;
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
        final CompletableFuture<Void> res;
        if (Key.ROOT.equals(key)) {
            res = new CompletableFutureSupport.Failed<Void>(
                new ArtipieIOException("Unable to save to root")
            ).get();
        } else {
            res = new Concatenation(new OneTimePublisher<>(content)).single()
                .to(SingleInterop.get())
                .thenApply(Remaining::new)
                .thenApply(Remaining::bytes)
                .thenAccept(
                    bytes -> {
                        synchronized (this.data) {
                            this.data.put(key.string(), bytes);
                        }
                    }
                ).toCompletableFuture();
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return CompletableFuture.runAsync(
            () -> {
                synchronized (this.data) {
                    final String key = source.string();
                    if (!this.data.containsKey(key)) {
                        throw new ArtipieIOException(
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
    public CompletableFuture<Long> size(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                synchronized (this.data) {
                    final byte[] content = this.data.get(key.string());
                    if (content == null) {
                        throw new ValueNotFoundException(key);
                    }
                    return (long) content.length;
                }
            }
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final CompletableFuture<Content> res;
        if (Key.ROOT.equals(key)) {
            res = new CompletableFutureSupport.Failed<Content>(
                new ArtipieIOException("Unable to load from root")
            ).get();
        } else {
            res = CompletableFuture.supplyAsync(
                () -> {
                    synchronized (this.data) {
                        final byte[] content = this.data.get(key.string());
                        if (content == null) {
                            throw new ValueNotFoundException(key);
                        }
                        return new Content.OneTime(new Content.From(content));
                    }
                }
            );
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return CompletableFuture.runAsync(
            () -> {
                synchronized (this.data) {
                    final String str = key.string();
                    if (!this.data.containsKey(str)) {
                        throw new ArtipieIOException(
                            String.format("Key does not exist: %s", str)
                        );
                    }
                    this.data.remove(str);
                }
            }
        );
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        return new UnderLockOperation<>(new StorageLock(this, key), operation).perform(this);
    }
}

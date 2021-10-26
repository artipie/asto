/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Fake storage for unit testing needs.
 *
 * @since 1.8.1
 */
public final class FakeStorage implements Storage {

    /**
     * Map for storing contents.
     */
    private final Map<Key, Content> map;

    /**
     * Ctor.
     */
    public FakeStorage() {
        this.map = new HashMap<>();
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> this.map.containsKey(key)
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return CompletableFuture.supplyAsync(
            () -> {
                final Collection<Key> keys = new LinkedList<>();
                for (final Key key : this.map.keySet()) {
                    if (key.string().startsWith(prefix.string())) {
                        keys.add(key);
                    }
                }
                return keys;
            }
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return CompletableFuture.supplyAsync(
            () -> {
                this.map.put(key, content);
                return null;
            }
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return CompletableFuture.supplyAsync(
            () -> {
                final Content content = this.map.get(source);
                this.map.remove(source);
                this.map.put(destination, content);
                return null;
            }
        );
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> this.map.get(key).size().get()
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> this.map.get(key)
        );
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                this.map.remove(key);
                return null;
            }
        );
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        if (!this.map.containsKey(key)) {
            throw new IllegalArgumentException("Key doesn't exist !");
        }
        return operation.apply(this);
    }
}

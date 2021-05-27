/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.bench;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.memory.InMemoryStorage;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.vavr.NotImplementedError;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

/**
 * Storage implementation for benchmarks.
 * @since 1.1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class BenchmarkStorage implements Storage {
    /**
     * Backend storage.
     */
    private final InMemoryStorage backend;

    /**
     * Local storage.
     */
    private final Map<Key, byte[]> local;

    /**
     * Set which contains deleted keys.
     */
    private final Set<Key> deleted;

    /**
     * Ctor.
     * @param backend Backend storage
     */
    public BenchmarkStorage(final InMemoryStorage backend) {
        this(backend, Collections.emptyMap());
    }

    /**
     * Ctor.
     * @param backend Backend storage
     * @param local Content of local storage
     */
    public BenchmarkStorage(final InMemoryStorage backend, final Map<Key, byte[]> local) {
        this.backend = backend;
        this.local = new ConcurrentSkipListMap<>(local);
        this.deleted = ConcurrentHashMap.newKeySet();
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        throw new NotImplementedError("Not implemented yet");
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
                .thenAccept(bytes -> this.local.put(key, bytes))
                .toCompletableFuture();
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return CompletableFuture.runAsync(
            () -> {
                if (this.deleted.contains(key)) {
                    throw new ValueNotFoundException(key);
                }
            }
        ).thenCompose(
            noth -> {
                final byte[] lcl = this.local.getOrDefault(key, null);
                final CompletableFuture<Content> res;
                if (lcl == null) {
                    res = this.backend.value(key);
                } else {
                    res = CompletableFuture.completedFuture(
                        new Content.OneTime(new Content.From(lcl))
                    );
                }
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        throw new NotImplementedError("Not implemented yet");
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        throw new NotImplementedError("Not implemented yet");
    }
}

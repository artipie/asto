/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */

package com.artipie.asto.etcd;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.ext.PublisherAs;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.GetOption.SortOrder;
import io.vavr.NotImplementedError;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Etcd based storage.
 * Main purpose of this storage is to be used as Artipie configuration main storage
 * for distributed cluster setup.
 * <p>
 * This storage loads content data into memory first, therefore
 * it has content size limitation for 10Mb. So it requires the client to
 * provide sized content.
 * </p>
 * @since 1.0
 * @checkstyle ReturnCountCheck (200 lines)
 */
public final class EtcdStorage implements Storage {

    /**
     * Reject content greater that 10MB.
     */
    private static final long MAX_SIZE = 1024 * 1024 * 10;

    /**
     * Etcd client.
     */
    private final Client client;

    /**
     * Ctor.
     * @param client Etcd client
     */
    public EtcdStorage(final Client client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return this.client.getKVClient().get(
            keyToSeq(key),
            GetOption.newBuilder().withCountOnly(true).build()
        ).thenApply(rsp -> rsp.getCount() > 0);
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return this.client.getKVClient().get(
            keyToSeq(prefix),
            GetOption.newBuilder()
                .withKeysOnly(true)
                .withSortOrder(SortOrder.ASCEND)
                .build()
        ).thenApply(
            rsp -> rsp.getKvs().stream()
                .map(kv -> new String(kv.getKey().getBytes(), StandardCharsets.UTF_8))
                .map(str -> new Key.From(str))
                .distinct()
                .collect(Collectors.toList())
        );
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final long size = content.size().orElse(0L);
        if (size <= 0 || size > EtcdStorage.MAX_SIZE) {
            return new CompletableFutureSupport.Failed<Void>(
                new ArtipieIOException(
                    String.format("Content size must be in range (1;%d)", EtcdStorage.MAX_SIZE)
                )
            ).get();
        }
        return new PublisherAs(content).bytes()
            .thenApply(ByteSequence::from)
            .thenCompose(data -> this.client.getKVClient().put(keyToSeq(key), data))
            .thenApply(ignore -> (Void) null).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return this.value(source)
            .thenCompose(data -> this.save(destination, data))
            .thenCompose(none -> this.delete(source));
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        return CompletableFuture.completedFuture(Meta.EMPTY);
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return this.client.getKVClient().get(keyToSeq(key)).thenApply(
            rsp -> rsp.getKvs().stream().max(
                (left, right) -> Long.compare(left.getVersion(), right.getVersion())
            )
        ).thenApply(
            kv -> kv.orElseThrow(
                () -> new ValueNotFoundException(key)
            ).getValue().getBytes()
        ).thenApply(bytes -> new Content.From(bytes));
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.client.getKVClient().delete(keyToSeq(key)).thenAccept(
            rsp -> {
                if (rsp.getDeleted() == 0) {
                    throw new ValueNotFoundException(key);
                }
            }
        );
    }

    @Override
    public <T> CompletionStage<T> exclusively(final Key key,
        final Function<Storage, CompletionStage<T>> operation) {
        throw new NotImplementedError("size not implemented");
    }

    /**
     * Convert asto key to ectd bytes.
     * @param key Asto key
     * @return Etcd byte sequence
     */
    private static ByteSequence keyToSeq(final Key key) {
        return ByteSequence.from(key.string(), StandardCharsets.UTF_8);
    }
}

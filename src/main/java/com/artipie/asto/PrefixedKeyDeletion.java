/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import java.util.concurrent.CompletableFuture;

/**
 * Deletion of items from a storage by a key prefix.
 * @since 1.9.1
 */
public class PrefixedKeyDeletion {

    /**
     * Targeted storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage to use
     */
    public PrefixedKeyDeletion(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Removes all items with key prefix.
     * @param prefix Key prefix
     * @return Completion or error signal.
     */
    public CompletableFuture<Void> remove(final Key prefix) {
        return this.storage.list(prefix).thenCompose(
            list -> CompletableFuture.allOf(
                list.stream().map(this.storage::delete)
                    .toArray(CompletableFuture[]::new)
            )
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.rx.RxCopy;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Storage synchronization.
 * @since 0.19
 * @checkstyle ParameterNameCheck (500 lines)
 */
public class Copy {

    /**
     * The storage to copy from.
     */
    private final Storage from;

    /**
     * The keys to transfer.
     */
    private final Optional<Collection<Key>> keys;

    /**
     * Ctor.
     *
     * @param from The storage to copy to.
     */
    public Copy(final Storage from) {
        this(from, Optional.empty());
    }

    /**
     * Ctor.
     * @param from The storage to copy to.
     * @param keys The keys to copy.
     */
    public Copy(final Storage from, final Collection<Key> keys) {
        this(from, Optional.of(keys));
    }

    /**
     * Ctor.
     *
     * @param from The storage to copy to.
     * @param keys The keys to copy.
     */
    private Copy(final Storage from, final Optional<Collection<Key>> keys) {
        this.from = from;
        this.keys = keys;
    }

    /**
     * Copy keys to the specified storage.
     * @param to The storage to copy to.
     * @return When copy operation completes
     */
    public CompletableFuture<Void> copy(final Storage to) {
        return this.keys
            .map(ks -> new RxCopy(new RxStorageWrapper(this.from), ks))
            .orElse(new RxCopy(new RxStorageWrapper(this.from)))
            .copy(new RxStorageWrapper(to))
            .to(CompletableInterop.await())
            .<Void>thenApply(o -> null)
            .toCompletableFuture();
    }
}

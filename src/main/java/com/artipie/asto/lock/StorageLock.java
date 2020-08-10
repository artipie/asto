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
package com.artipie.asto.lock;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * {@link Lock} allowing to obtain lock on target {@link Key} in specified {@link Storage}.
 * Lock is identified by it's unique identifier (UUID), which has to be different for each lock.
 *
 * @since 0.24
 */
public final class StorageLock implements Lock {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Target key.
     */
    private final Key target;

    /**
     * Identifier.
     */
    private final String uuid;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     */
    public StorageLock(final Storage storage, final Key target) {
        this(storage, target, UUID.randomUUID().toString());
    }

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     * @param uuid Identifier.
     */
    public StorageLock(final Storage storage, final Key target, final String uuid) {
        this.storage = storage;
        this.target = target;
        this.uuid = uuid;
    }

    @Override
    public CompletableFuture<Void> acquire() {
        final Key root = new ProposalsKey(this.target);
        final Key proposal = new Key.From(root, this.uuid);
        return this.storage.save(proposal, new Content.From(new byte[] {})).thenCompose(
            nothing -> this.storage.list(root).thenCompose(
                proposals -> {
                    if (proposals.size() != 1 || !proposals.contains(proposal)) {
                        throw new IllegalStateException(
                            String.format(
                                "Failed to acquire lock. Own: `%s` Found: %s",
                                proposal,
                                proposals.stream()
                                    .map(Key::toString)
                                    .map(str -> String.format("`%s`", str))
                                    .collect(Collectors.joining(", "))
                            )
                        );
                    }
                    return CompletableFuture.allOf();
                }
            )
        );
    }

    @Override
    public CompletableFuture<Void> release() {
        throw new UnsupportedOperationException();
    }

    /**
     * Root key for lock proposals.
     *
     * @since 0.24
     */
    static class ProposalsKey extends Key.Wrap {

        /**
         * Ctor.
         *
         * @param target Target key.
         */
        protected ProposalsKey(final Key target) {
            super(new Key.From(new Key.From(".artipie-locks"), new Key.From(target)));
        }
    }
}

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
package com.artipie.asto.lock.storage;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.lock.Lock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * {@link Lock} allowing to obtain lock on target {@link Key} in specified {@link Storage}.
 * Lock is identified by it's unique identifier (UUID), which has to be different for each lock.
 *
 * @since 0.24
 */
public final class StorageLock implements Lock {

    /**
     * Proposals.
     */
    private final Proposals proposals;

    /**
     * Identifier.
     */
    private final String uuid;

    /**
     * Expiration time.
     */
    private final Optional<Instant> expiration;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     */
    public StorageLock(final Storage storage, final Key target) {
        this(storage, target, UUID.randomUUID().toString(), Optional.empty());
    }

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     * @param expiration Expiration time.
     */
    public StorageLock(final Storage storage, final Key target, final Instant expiration) {
        this(storage, target, UUID.randomUUID().toString(), Optional.of(expiration));
    }

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     * @param uuid Identifier.
     * @param expiration Expiration time.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public StorageLock(
        final Storage storage,
        final Key target,
        final String uuid,
        final Optional<Instant> expiration
    ) {
        this.proposals = new Proposals(storage, target);
        this.uuid = uuid;
        this.expiration = expiration;
    }

    @Override
    public CompletionStage<Void> acquire() {
        return this.proposals.create(this.uuid, this.expiration).thenCompose(
            nothing -> this.proposals.checkSingle(this.uuid)
        ).handle(
            (nothing, throwable) -> {
                final CompletionStage<Void> result;
                if (throwable == null) {
                    result = CompletableFuture.allOf();
                } else {
                    result = this.release().thenCompose(
                        released -> {
                            final CompletableFuture<Void> failed = new CompletableFuture<>();
                            failed.completeExceptionally(throwable);
                            return failed;
                        }
                    );
                }
                return result;
            }
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletionStage<Void> release() {
        return this.proposals.delete(this.uuid);
    }
}

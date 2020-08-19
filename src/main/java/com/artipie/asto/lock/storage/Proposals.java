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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Proposals for acquiring storage lock.
 *
 * @since 0.24
 */
final class Proposals {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Target key.
     */
    private final Key target;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     */
    Proposals(final Storage storage, final Key target) {
        this.storage = storage;
        this.target = target;
    }

    /**
     * Create proposal with specified UUID.
     *
     * @param uuid UUID.
     * @param expiration Expiration time.
     * @return Completion of proposal create operation.
     */
    public CompletionStage<Void> create(final String uuid, final Optional<Instant> expiration) {
        return this.storage.save(
            this.proposalKey(uuid),
            expiration.<Content>map(
                instant -> new Content.From(instant.toString().getBytes(StandardCharsets.US_ASCII))
            ).orElse(Content.EMPTY)
        );
    }

    /**
     * Check that there is single proposal with specified UUID.
     *
     * @param uuid UUID.
     * @return Completion of proposals check operation.
     */
    public CompletionStage<Void> checkSingle(final String uuid) {
        final Instant now = Instant.now();
        final Key own = this.proposalKey(uuid);
        return this.storage.list(new RootKey(this.target)).thenCompose(
            proposals -> CompletableFuture.allOf(
                proposals.stream()
                    .filter(key -> !key.equals(own))
                    .map(
                        proposal -> this.storage.value(proposal)
                            .thenApply(PublisherAs::new)
                            .thenCompose(PublisherAs::asciiString)
                            .thenCompose(
                                expiration -> {
                                    if (isNotExpired(expiration, now)) {
                                        throw new IllegalStateException(
                                            String.join(
                                                "\n",
                                                "Failed to acquire lock.",
                                                String.format("Own: `%s`", own),
                                                String.format(
                                                    "Others: %s",
                                                    proposals.stream()
                                                        .map(Key::toString)
                                                        .map(str -> String.format("`%s`", str))
                                                        .collect(Collectors.joining(", "))
                                                ),
                                                String.format(
                                                    "Not expired: `%s` `%s`",
                                                    proposal,
                                                    expiration
                                                )
                                            )
                                        );
                                    }
                                    return CompletableFuture.allOf();
                                }
                            )
                    )
                    .toArray(CompletableFuture[]::new)
            )
        );
    }

    /**
     * Delete proposal with specified UUID.
     *
     * @param uuid UUID.
     * @return Completion of proposal delete operation.
     */
    public CompletionStage<Void> delete(final String uuid) {
        return this.storage.delete(this.proposalKey(uuid));
    }

    /**
     * Construct proposal key with specified UUID.
     *
     * @param uuid UUID.
     * @return Proposal key.
     */
    private Key proposalKey(final String uuid) {
        return new Key.From(new RootKey(this.target), uuid);
    }

    /**
     * Checks that instant in string format is not expired, e.g. is after current time.
     * Empty string considered to never expire.
     *
     * @param instant Instant in string format.
     * @param now Current time.
     * @return True if instant is not expired, false - otherwise.
     */
    private static boolean isNotExpired(final String instant, final Instant now) {
        return instant.isEmpty() || Instant.parse(instant).isAfter(now);
    }

    /**
     * Root key for lock proposals.
     *
     * @since 0.24
     */
    static class RootKey extends Key.Wrap {

        /**
         * Ctor.
         *
         * @param target Target key.
         */
        protected RootKey(final Key target) {
            super(new From(new From(".artipie-locks"), new From(target)));
        }
    }
}

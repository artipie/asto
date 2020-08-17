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
import com.artipie.asto.memory.InMemoryStorage;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test cases for {@link StorageLock}.
 *
 * @since 0.24
 */
@Timeout(1)
final class StorageLockTest {

    /**
     * Storage used in tests.
     */
    private final InMemoryStorage storage = new InMemoryStorage();

    /**
     * Lock target key.
     */
    private final Key target = new Key.From("a/b/c");

    @Test
    void shouldAddValueWhenAcquiredLock() {
        final String uuid = UUID.randomUUID().toString();
        new StorageLock(this.storage, this.target, uuid).acquire().toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.exists(new Key.From(new Proposals.RootKey(this.target), uuid))
                .toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldAcquireWhenValuePresents() {
        final String uuid = UUID.randomUUID().toString();
        this.storage.save(
            new Key.From(new Proposals.RootKey(this.target), uuid),
            Content.EMPTY
        ).toCompletableFuture().join();
        final StorageLock lock = new StorageLock(this.storage, this.target, uuid);
        Assertions.assertDoesNotThrow(() -> lock.acquire().toCompletableFuture().join());
    }

    @Test
    void shouldFailAcquireLockIfOtherProposalExists() {
        final String uuid = UUID.randomUUID().toString();
        final Key proposal = new Key.From(new Proposals.RootKey(this.target), uuid);
        this.storage.save(proposal, Content.EMPTY).toCompletableFuture().join();
        final StorageLock lock = new StorageLock(this.storage, this.target);
        final CompletionException exception = Assertions.assertThrows(
            CompletionException.class,
            () -> lock.acquire().toCompletableFuture().join(),
            "Fails to acquire"
        );
        MatcherAssert.assertThat(
            "Reason for failure is IllegalStateException",
            exception.getCause(),
            new IsInstanceOf(IllegalStateException.class)
        );
        MatcherAssert.assertThat(
            "Proposals unmodified",
            this.storage.list(new Proposals.RootKey(this.target))
                .toCompletableFuture().join()
                .stream()
                .map(Key::string)
                .collect(Collectors.toList()),
            Matchers.contains(proposal.string())
        );
    }

    @Test
    void shouldRemoveProposalOnRelease() {
        final String uuid = UUID.randomUUID().toString();
        final Key proposal = new Key.From(new Proposals.RootKey(this.target), uuid);
        this.storage.save(proposal, Content.EMPTY).toCompletableFuture().join();
        new StorageLock(this.storage, this.target, uuid).release().toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.exists(proposal).toCompletableFuture().join(),
            new IsEqual<>(false)
        );
    }
}

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
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link FromStorageCache}.
 *
 * @since 0.24
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class FromStorageCacheTest {

    /**
     * Storage for tests.
     */
    private final Storage storage = new InMemoryStorage();

    @Test
    void loadsFromCache() throws Exception {
        final Key key = new Key.From("key1");
        final byte[] data = "hello1".getBytes();
        new BlockingStorage(this.storage).save(key, data);
        MatcherAssert.assertThat(
            new FromStorageCache(this.storage).load(
                key,
                new Remote.Failed(new IllegalStateException("Failing remote 1")),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().get().get(),
            new ContentIs(data)
        );
    }

    @Test
    void savesToCacheFromRemote() throws Exception {
        final Key key = new Key.From("key2");
        final byte[] data = "hello2".getBytes();
        final FromStorageCache cache = new FromStorageCache(this.storage);
        final Content load = cache.load(
            key,
            () -> CompletableFuture.supplyAsync(() -> Optional.of(new Content.From(data))),
            CacheControl.Standard.ALWAYS
        ).toCompletableFuture().get().get();
        MatcherAssert.assertThat(
            "Cache returned broken remote content",
            load, new ContentIs(data)
        );
        MatcherAssert.assertThat(
            "Cache didn't save remote content locally",
            cache.load(
                key,
                new Remote.Failed(new IllegalStateException("Failing remote 1")),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().get().get(),
            new ContentIs(data)
        );
    }

    @Test
    void dontCacheFailedRemote() throws Exception {
        final Key key = new Key.From("key3");
        final AtomicInteger cnt = new AtomicInteger();
        new FromStorageCache(this.storage).load(
            key,
            () -> CompletableFuture.supplyAsync(
                () -> Optional.of(
                    new Content.From(
                        Flowable.generate(
                            emitter -> {
                                if (cnt.incrementAndGet() < 3) {
                                    emitter.onNext(ByteBuffer.allocate(4));
                                } else {
                                    emitter.onError(new Exception("Error!"));
                                }
                            }
                        )
                    )
                )
            ),
            CacheControl.Standard.ALWAYS
        ).exceptionally(
            err -> {
                Logger.info(this, "Handled error: %s", err.getMessage());
                return null;
            }
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).exists(key), Matchers.is(false)
        );
    }

    @Test
    void processMultipleRequestsSimultaneously() throws Exception {
        final FromStorageCache cache = new FromStorageCache(this.storage);
        final Key key = new Key.From("key4");
        final int count = 100;
        final CountDownLatch latch = new CountDownLatch(
            Runtime.getRuntime().availableProcessors() - 1
        );
        final byte[] data = "data".getBytes();
        final Remote remote =
            () -> CompletableFuture
                .runAsync(
                    () -> {
                        latch.countDown();
                        try {
                            latch.await();
                        } catch (final InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(ex);
                        }
                    })
                .thenApply(nothing -> ByteBuffer.wrap(data))
                .thenApply(Flowable::just)
                .thenApply(Content.From::new)
                .thenApply(Optional::of);
        Observable.range(0, count).flatMapCompletable(
            num -> SingleInterop.fromFuture(cache.load(key, remote, CacheControl.Standard.ALWAYS))
                .flatMapCompletable(
                    pub -> CompletableInterop.fromFuture(
                        this.storage.save(new Key.From("out", num.toString()), pub.get())
                    )
                )
        ).blockingAwait();
        for (int num = 0; num < count; ++num) {
            MatcherAssert.assertThat(
                new BlockingStorage(this.storage).value(new Key.From("out", String.valueOf(num))),
                Matchers.equalTo(data)
            );
        }
    }
}

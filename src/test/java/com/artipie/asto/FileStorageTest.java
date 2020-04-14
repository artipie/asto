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
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.codec.binary.Hex;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link Storage}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
final class FileStorageTest {

    /**
     * Vert.x used to create tested FileStorage.
     */
    private Vertx vertx;

    /**
     * File storage used in tests.
     */
    private FileStorage storage;

    @BeforeEach
    void setUp(@TempDir final Path tmp) {
        this.vertx = Vertx.vertx();
        this.storage = new FileStorage(tmp, this.vertx);
    }

    @AfterEach
    void tearDown() {
        if (this.vertx != null) {
            this.vertx.rxClose().blockingAwait();
        }
    }

    @Test
    void savesAndLoads() throws Exception {
        final byte[] content = "Hello world!!!".getBytes();
        final Key key = new Key.From("a", "b", "test.deb");
        this.storage.save(
            key,
            new Content.From(content)
        ).get();
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(this.storage.value(key).get()).single().blockingGet(),
                true
            ).bytes(),
            Matchers.equalTo(content)
        );
    }

    @Test
    void saveOverwrites() {
        final byte[] original = "1".getBytes(StandardCharsets.UTF_8);
        final byte[] updated = "2".getBytes(StandardCharsets.UTF_8);
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Key key = new Key.From("foo");
        blocking.save(key, original);
        blocking.save(key, updated);
        MatcherAssert.assertThat(
            blocking.value(key),
            new IsEqual<>(updated)
        );
    }

    @Test
    void blockingWrapperWorks() {
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final String content = "hello, friend!";
        final Key key = new Key.From("t", "y", "testb.deb");
        blocking.save(
            key, new ByteArray(content.getBytes(StandardCharsets.UTF_8)).primitiveBytes()
        );
        final byte[] bytes = blocking.value(key);
        MatcherAssert.assertThat(
            new String(bytes, StandardCharsets.UTF_8),
            Matchers.equalTo(content)
        );
    }

    @Test
    void move() {
        final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Key source = new Key.From("from");
        blocking.save(source, data);
        final Key destination = new Key.From("to");
        blocking.move(source, destination);
        MatcherAssert.assertThat(
            blocking.value(destination),
            Matchers.equalTo(data)
        );
    }

    @Test
    @EnabledIfSystemProperty(named = "test.storage.file.huge", matches = "true|on")
    void saveAndLoadHugeFiles() throws Exception {
        // @checkstyle MagicNumberCheck (30 lines)
        // @checkstyle MethodBodyCommentsCheck (30 lines)
        final Key key = new Key.From("huge");
        // one 8KB fragment of repeated sequence of byte ranges 0x00..0xFF
        final ByteBuffer fragment = ByteBuffer.wrap(
            new ByteArray(
                IntStream.range(0, 4 * 8).flatMap(
                    cnt -> IntStream.range(0, 256)
                ).mapToObj(x -> (byte) x)
                    .collect(Collectors.toList())
            ).primitiveBytes()
        );
        // 1 GB of 8KB byte fragments
        final AtomicLong cnt = new AtomicLong(131_072);
        this.storage.save(
            key,
            new Content.From(
                Flowable.generate(
                    // @checkstyle ReturnCountCheck (10 lines)
                    emitter -> {
                        if (cnt.decrementAndGet() == 0) {
                            emitter.onComplete();
                            return;
                        }
                        if (cnt.get() == 0) {
                            return;
                        }
                        emitter.onNext(fragment.slice());
                    }
                )
            )
        ).get();
        final String hash = this.storage.value(key).thenCompose(
            content -> Flowable.fromPublisher(content).reduceWith(
                () -> MessageDigest.getInstance("SHA256"),
                (digest, buf) -> {
                    digest.update(buf);
                    return digest;
                }
            ).map(digest -> new String(Hex.encodeHex(digest.digest())))
                .to(SingleInterop.get())
        ).get();
        MatcherAssert.assertThat(
            hash,
            new IsEqual<>("13b0e1029eae62b2cde3e918d384ce704319a1eca1cf268b962cb34dbbab04e4")
        );
    }
}

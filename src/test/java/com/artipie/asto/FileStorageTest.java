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
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
     * File storage used in tests.
     */
    private FileStorage storage;

    @BeforeEach
    void setUp(@TempDir final Path tmp) {
        this.storage = new FileStorage(tmp);
    }

    @Test
    void savesAndLoads() throws Exception {
        final byte[] content = "Hello world!!!".getBytes();
        final Key key = new Key.From("a", "b", "test.deb");
        this.storage.save(
            key,
            new Content.OneTime(new Content.From(content))
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
    void saveOverwrites() throws Exception {
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
    void saveBadContentDoesNotLeaveTrace() {
        this.storage.save(
            new Key.From("a/b/c/"),
            new Content.From(Flowable.error(new IllegalStateException()))
        ).exceptionally(ignored -> null).join();
        MatcherAssert.assertThat(
            this.storage.list(Key.ROOT).join(),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void readsTheSize() throws Exception {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key key = new Key.From("withSize");
        bsto.save(key, new byte[]{0x00, 0x00, 0x00});
        MatcherAssert.assertThat(
            bsto.size(key),
            // @checkstyle MagicNumberCheck (1 line)
            Matchers.equalTo(3L)
        );
    }

    @Test
    void blockingWrapperWorks() throws Exception {
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
    void move() throws Exception {
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
    @Timeout(1L)
    void saveAndLoadHugeFiles(@TempDir final Path tmp) throws Exception {
        final String name = "huge";
        new FileStorage(tmp).save(
            new Key.From(name),
            new Content.OneTime(
                new Content.From(
                    // @checkstyle MagicNumberCheck (1 line)
                    Flowable.generate(new WriteTestSource(1024 * 8, 1024 * 1024 / 8))
                )
            )
        ).get();
        MatcherAssert.assertThat(
            Files.size(tmp.resolve(name)),
            // @checkstyle MagicNumberCheck (1 line)
            Matchers.equalTo(1024L * 1024 * 1024)
        );
    }

    /**
     * Provider of byte buffers for write test.
     * @since 0.2
     */
    private static final class WriteTestSource implements Consumer<Emitter<ByteBuffer>> {

        /**
         * Counter.
         */
        private final AtomicInteger cnt;

        /**
         * Amount of buffers.
         */
        private final int length;

        /**
         * Buffer size.
         */
        private final int size;

        /**
         * New test source.
         * @param size Buffer size
         * @param length Amount of buffers
         */
        WriteTestSource(final int size, final int length) {
            this.cnt = new AtomicInteger();
            this.size = size;
            this.length = length;
        }

        @Override
        public void accept(final Emitter<ByteBuffer> src) {
            final int val = this.cnt.getAndIncrement();
            if (val < this.length) {
                final byte[] data = new byte[this.size];
                Arrays.fill(data, (byte) val);
                src.onNext(ByteBuffer.wrap(data));
            } else {
                src.onComplete();
            }
        }
    }
}

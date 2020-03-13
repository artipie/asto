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
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link Storage}.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
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
        this.storage = new FileStorage(tmp, this.vertx.fileSystem());
    }

    @AfterEach
    void tearDown() {
        if (this.vertx != null) {
            this.vertx.rxClose().blockingAwait();
        }
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void savesAndLoads() throws Exception {
        final String content = "Hello world!!!";
        final Key key = new Key.From("a", "b", "test.deb");
        this.storage.save(
            key,
            new Content.From(content.getBytes())
        ).get();
        MatcherAssert.assertThat(
            new String(
                new ByteArray(Flowable.fromPublisher(this.storage.value(key).get())
                    .toList()
                    .blockingGet()
                    .stream()
                    .map(buf -> new Remaining(buf).bytes())
                    .flatMap(byteArr -> Arrays.stream(new ByteArray(byteArr).boxedBytes()))
                    .toArray(Byte[]::new)
                ).primitiveBytes()
            ),
            Matchers.equalTo(content)
        );
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void saveOverwrites() {
        final byte[] original = "1".getBytes();
        final byte[] updated = "2".getBytes();
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Key key = new Key.From("foo");
        blocking.save(key, original);
        blocking.save(key, updated);
        MatcherAssert.assertThat(
            blocking.value(key),
            new IsEqual<>(updated)
        );
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void blockingWrapperWorks() {
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final String content = "hello, friend!";
        final Key key = new Key.From("t", "y", "testb.deb");
        blocking.save(key, new ByteArray(content.getBytes()).primitiveBytes());
        final byte[] bytes = blocking.value(key);
        MatcherAssert.assertThat(
            new String(bytes),
            Matchers.equalTo(content)
        );
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void move() {
        final byte[] data = "data".getBytes();
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
    void list() {
        final byte[] data = "some data!".getBytes();
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        blocking.save(new Key.From("a", "b", "c", "1"), data);
        blocking.save(new Key.From("a", "b", "2"), data);
        blocking.save(new Key.From("a", "z"), data);
        blocking.save(new Key.From("z"), data);
        final Collection<String> keys = blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
        );
    }

    @Test
    void listEmpty() {
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Collection<String> keys = blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            Matchers.empty()
        );
    }

    @Test
    void shouldExistForSavedKey() {
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Key key = new Key.From("some", "key");
        blocking.save(key, "some data".getBytes());
        MatcherAssert.assertThat(
            blocking.exists(key),
            Matchers.equalTo(true)
        );
    }

    @Test
    void shouldNotExistForUnknownKey() throws Exception {
        MatcherAssert.assertThat(
            this.storage.exists(new Key.From("unknown")).get(),
            Matchers.equalTo(false)
        );
    }

    @Test
    void shouldNotExistForParentOfSavedKey() {
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final Key parent = new Key.From("a", "b");
        final Key key = new Key.From(parent, "c");
        final byte[] data = "content".getBytes();
        blocking.save(key, data);
        MatcherAssert.assertThat(
            blocking.exists(parent),
            Matchers.equalTo(false)
        );
    }

    @Test
    void shouldDeleteValue() throws Exception {
        final Key key = new Key.From("shouldDeleteValue");
        final byte[] data = "shouldDeleteValue-data".getBytes();
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        blocking.save(key, data);
        blocking.delete(key);
        MatcherAssert.assertThat(
            this.storage.exists(key).get(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldFailToDeleteNotExistingValue() {
        final Key key = new Key.From("shouldFailToDeleteNotExistingValue");
        Assertions.assertThrows(Exception.class, () -> this.storage.delete(key).get());
    }

    @Test
    void shouldFailToDeleteParentOfSavedKey() {
        final Key parent = new Key.From("shouldFailToDeleteParentOfSavedKey");
        final Key key = new Key.From(parent, "child");
        final byte[] content = "shouldFailToDeleteParentOfSavedKey-content".getBytes();
        new BlockingStorage(this.storage).save(key, content);
        Assertions.assertThrows(Exception.class, () -> this.storage.delete(parent).get());
    }
}

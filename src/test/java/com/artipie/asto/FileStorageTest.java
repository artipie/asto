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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.RepeatedTest;
import org.reactivestreams.FlowAdapters;

/**
 * Test case for {@link Storage}.
 * @since 0.1
 */
final class FileStorageTest {

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void savesAndLoads() throws Exception {
        final Vertx vertx = Vertx.vertx();
        final Path tmp = Files.createTempDirectory("tmp-save");
        final Storage storage = new FileStorage(tmp, vertx.fileSystem());
        final String content = "Hello world!!!";
        final Key key = new Key.From("a", "b", "test.deb");
        storage.save(
            key,
            FlowAdapters.toFlowPublisher(
                Flowable.fromArray(
                    new ByteArray(content.getBytes()).boxedBytes()
                ).map(
                    b -> {
                        final ByteBuffer buf = ByteBuffer.allocate(1);
                        buf.put(b);
                        buf.rewind();
                        return buf;
                    })
            )
        ).get();
        MatcherAssert.assertThat(
            new String(
                new ByteArray(Flowable.fromPublisher(
                    FlowAdapters.toPublisher(
                        storage.value(key).get()
                    )
                )
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
        MatcherAssert.assertThat(tmp.toFile().delete(), new IsEqual<>(true));
        vertx.rxClose().blockingAwait();
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void saveOverwrites() throws IOException {
        final Vertx vertx = Vertx.vertx();
        final Path tmp = Files.createTempDirectory("tmp-save-over-writes");
        tmp.toFile().deleteOnExit();
        final byte[] original = "1".getBytes();
        final byte[] updated = "2".getBytes();
        final BlockingStorage storage = new BlockingStorage(
            new FileStorage(tmp, vertx.fileSystem())
        );
        final Key key = new Key.From("foo");
        storage.save(key, original);
        storage.save(key, updated);
        MatcherAssert.assertThat(
            "Value read from storage should be updated",
            storage.value(key),
            new IsEqual<>(updated)
        );
        vertx.rxClose().blockingAwait();
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void blockingWrapperWorks() throws IOException {
        final Vertx vertx = Vertx.vertx();
        final Path tmp = Files.createTempDirectory("tmp-blocking");
        tmp.toFile().deleteOnExit();
        final BlockingStorage storage = new BlockingStorage(
            new FileStorage(tmp, vertx.fileSystem())
        );
        final String content = "hello, friend!";
        final Key key = new Key.From("t", "y", "testb.deb");
        storage.save(key, new ByteArray(content.getBytes()).primitiveBytes());
        final byte[] bytes = storage.value(key);
        MatcherAssert.assertThat(
            new String(bytes),
            Matchers.equalTo(content)
        );
        vertx.rxClose().blockingAwait();
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    void move() throws IOException {
        final Vertx vertx = Vertx.vertx();
        final Path tmp = Files.createTempDirectory("tmp-move");
        tmp.toFile().deleteOnExit();
        final byte[] data = "data".getBytes();
        final BlockingStorage storage = new BlockingStorage(
            new FileStorage(tmp, vertx.fileSystem())
        );
        final Key source = new Key.From("from");
        storage.save(source, data);
        final Key destination = new Key.From("to");
        storage.move(source, destination);
        MatcherAssert.assertThat(storage.value(destination), Matchers.equalTo(data));
        vertx.rxClose().blockingAwait();
    }
}

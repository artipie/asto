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

import com.artipie.asto.fs.RxFile;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link RxFile}.
 * @since 0.11.1
 */
final class RxFileTest {

    @Test
    public void rxFileFlowWorks(@TempDir final Path tmp) throws IOException {
        final Vertx vertx = Vertx.vertx();
        final String hello = "hello-world";
        final Path temp = tmp.resolve("txt-file");
        Files.write(temp, hello.getBytes());
        final String content = new RxFile(temp, vertx.fileSystem())
            .flow()
            .rebatchRequests(1)
            .toList()
            .map(
                list -> list.stream().map(buf -> new Remaining(buf).bytes())
                    .flatMap(byteArr -> Arrays.stream(new ByteArray(byteArr).boxedBytes()))
                    .toArray(Byte[]::new)
            )
            .map(bytes -> new String(new ByteArray(bytes).primitiveBytes()))
            .blockingGet();
        MatcherAssert.assertThat(hello, Matchers.equalTo(content));
        vertx.close();
    }

    @Test
    public void rxFileTruncatesExistingFile(@TempDir final Path tmp) throws Exception {
        final Vertx vertx = Vertx.vertx();
        final String one = "one";
        final String two = "two111";
        final Path target = tmp.resolve("target.txt");
        new RxFile(target, vertx.fileSystem()).save(pubFromString(two)).blockingAwait();
        new RxFile(target, vertx.fileSystem()).save(pubFromString(one)).blockingAwait();
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(target), StandardCharsets.UTF_8),
            Matchers.equalTo(one)
        );
        vertx.close();
    }

    // @checkstyle MagicNumberCheck (1 line)
    @RepeatedTest(100)
    public void rxFileSaveWorks(@TempDir final Path tmp) throws IOException {
        final Vertx vertx = Vertx.vertx();
        final String hello = "hello-world!!!";
        final Path temp = tmp.resolve("saved.txt");
        new RxFile(temp, vertx.fileSystem())
            .save(
                Flowable.fromArray(new ByteArray(hello.getBytes()).boxedBytes()).map(
                    aByte -> {
                        final byte[] bytes = new byte[1];
                        bytes[0] = aByte;
                        return ByteBuffer.wrap(bytes);
                    }
                )
            ).blockingAwait();
        MatcherAssert.assertThat(new String(Files.readAllBytes(temp)), Matchers.equalTo(hello));
        vertx.close();
    }

    @Test()
    public void rxFileSizeWorks(@TempDir final Path tmp) throws IOException {
        final Vertx vertx = Vertx.vertx();
        final byte[] data = "012345".getBytes();
        final Path temp = tmp.resolve("size-test.txt");
        Files.write(temp, data);
        final Long size = new RxFile(temp, vertx.fileSystem()).size().blockingGet();
        MatcherAssert.assertThat(
            size,
            Matchers.equalTo((long) data.length)
        );
        vertx.close();
    }

    /**
     * Creates publisher of byte buffers from string using UTF8 encoding.
     * @param str Source string
     * @return Publisher
     */
    private static Flowable<ByteBuffer> pubFromString(final String str) {
        return Flowable.fromArray(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
    }
}

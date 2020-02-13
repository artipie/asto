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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RxFile}.
 * @since 0.11.1
 */
final class RxFileTest {

    @Test
    public void rxFileFlowWorks() throws IOException {
        final String hello = "hello-world";
        final Path temp = Files.createTempFile(hello, ".txt");
        Files.write(temp, hello.getBytes());
        final String content = new RxFile(temp)
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
        temp.toFile().deleteOnExit();
    }

    @Test
    public void rxFileSaveWorks() throws IOException {
        final Vertx vertx = Vertx.vertx();
        final String hello = "hello-world!!!";
        final Path temp = Files.createTempFile(hello, "saved.txt");
        for (int idx = 0; idx < 100; idx += 1) {
            temp.toFile().delete();
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
        }
        vertx.close();
    }
}

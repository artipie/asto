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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.reactivestreams.FlowAdapters;

/**
 * Test case for {@link Storage}.
 *
 * @since 0.1
 */
public final class FileStorageTest {

    /**
     * Temp folder for all tests.
     */
    @Rule
    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Fake storage works.
     * @throws Exception If some problem inside
     */
    @Test
    public void savesAndLoads() throws Exception {
        final Storage storage = new FileStorage(Files.createTempDirectory("temp"));
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
    }

    @Test
    public void blockingWrapperWorks() throws IOException {
        final BlockingStorage storage = new BlockingStorage(
            new FileStorage(
                Files.createTempDirectory("temp-blocking")
            )
        );
        final String content = "hello, friend!";
        final Key key = new Key.From("t", "y", "testb.deb");
        storage.save(key, new ByteArray(content.getBytes()).primitiveBytes());
        final byte[] bytes = storage.value(key);
        MatcherAssert.assertThat(
            new String(bytes),
            Matchers.equalTo(content)
        );
    }
}

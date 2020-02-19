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

package com.artipie.asto.io;

import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link RxByteBuffers}.
 * @since 0.14
 */
public final class RxByteBuffersTest {

    /**
     * Test temporary directory.
     * By JUnit annotation contract it should not be private
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path temp;

    @Test
    public void readChannel() throws IOException {
        final Path file = this.temp.resolve("readChannel.txt");
        final String line = this.randomString();
        Files.writeString(file, line);
        MatcherAssert.assertThat(
            line,
            new IsEqual<>(
                this.flow(
                    new RxByteBuffers().path(file)
                ).toString()
            )
        );
    }

    @Test
    public void writeChannel() throws IOException {
        final Path file = this.temp.resolve("writeChannel.txt");
        final String line = this.randomString();
        new RxByteBuffers().path(this.wrap(line.getBytes()), file)
            .blockingSubscribe();
        MatcherAssert.assertThat(
            Files.readString(file),
            new IsEqual<>(line)
        );
    }

    private String randomString() {
        return UUID.randomUUID().toString();
    }

    private Flowable<ByteBuffer> wrap(final byte[] array) {
        return Flowable.fromArray(array).map(ByteBuffer::wrap);
    }

    private ByteArrayOutputStream flow(final Flowable<ByteBuffer> bytes) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (final ByteBuffer buffer: bytes.blockingIterable()) {
            final byte[] array = new byte[buffer.remaining()];
            buffer.get(array);
            baos.write(array);
        }
        return baos;
    }
}

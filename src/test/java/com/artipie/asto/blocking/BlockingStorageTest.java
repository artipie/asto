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

package com.artipie.asto.blocking;

import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Debug test for {@link BlockingStorage}.
 * @since 0.14
 */
public final class BlockingStorageTest {

    /**
     * Temporary directory.
     */
    private Path temp;

    /**
     * A file inside of {@link #temp}.
     */
    private Path file;

    @BeforeEach
    public void before() throws IOException {
        this.temp = Files.createTempDirectory("BlockingStorageTest");
        this.file = this.temp.resolve("file.txt");
    }

    @AfterEach
    public void after() throws IOException {
        try {
            Files.deleteIfExists(this.file);
            Files.deleteIfExists(this.temp);
        } catch (final IOException ex) {
            final List<String> files = Files.walk(this.temp)
                .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                .map(Object::toString)
                .collect(Collectors.toList());
            throw new IOException(files.toString(), ex);
        }
    }

    @Test
    void shouldFailOnCleanup() throws IOException {
        final String content = "Hello world";
        Files.writeString(
            this.file,
            content,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
        final BlockingStorage asto = new BlockingStorage(new FileStorage(this.temp));
        MatcherAssert.assertThat(
            new String(asto.value(new Key.From(this.file.getFileName().toString()))),
            new IsEqual<>(content)
        );
    }
}

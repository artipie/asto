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
package com.artipie.asto.test;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import io.vertx.reactivex.core.file.FileSystem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Test resource.
 * @since 0.24
 */
public final class TestResource {

    /**
     * Relative to test resources folder resource path.
     */
    private final String name;

    /**
     * The Vert.x file system.
     */
    private final FileSystem fls;

    /**
     * Ctor.
     * @param name Resource path
     * @param fls The file system.
     */
    public TestResource(final String name, final FileSystem fls) {
        this.name = name;
        this.fls = fls;
    }

    /**
     * Reads recourse and saves it to storage by given key.
     * @param storage Where to save
     * @param key Key to save by
     */
    public void saveTo(final Storage storage, final Key key) {
        storage.save(key, new Content.From(this.asBytes())).join();
    }

    /**
     * Reads recourse and saves it to storage by given path as a key.
     * @param storage Where to save
     */
    public void saveTo(final Storage storage) {
        this.saveTo(storage, new Key.From(this.name));
    }

    /**
     * Adds files from resources (specified folder) to storage, storage items keys are constructed
     * from the `base` key and filename.
     * @param storage Where to save
     * @param base Base key
     */
    public void addFilesTo(final Storage storage, final Key base) {
        final Storage resources = new FileStorage(this.asPath(), this.fls);
        resources.list(Key.ROOT).thenCompose(
            keys -> CompletableFuture.allOf(
                keys.stream().map(Key::string).map(
                    item -> resources.value(new Key.From(item)).thenCompose(
                        content -> storage.save(new Key.From(base, item), content)
                    )
                ).toArray(CompletableFuture[]::new)
            )
        ).join();
    }

    /**
     * Obtains resources from context loader.
     * @return File path
     */
    public Path asPath() {
        try {
            return Paths.get(
                Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(this.name)
                ).toURI()
            );
        } catch (final URISyntaxException ex) {
            throw new IllegalStateException("Failed to obtain test recourse", ex);
        }
    }

    /**
     * Recourse as Input stream.
     * @return Input stream
     */
    public InputStream asInputStream() {
        return Objects.requireNonNull(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(this.name)
        );
    }

    /**
     * Recourse as bytes.
     * @return Bytes
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public byte[] asBytes() {
        try (InputStream stream = this.asInputStream()) {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int count;
            final byte[] data = new byte[8 * 1024];
            while ((count = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, count);
            }
            return buffer.toByteArray();
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load test recourse", ex);
        }
    }
}

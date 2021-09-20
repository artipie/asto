/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

/**
 * Test for {@link StorageValueAsStream}.
 * @since 1.5
 */
class ProcessContentTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void processesExistingItem() {
        final Key key = new Key.From("test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        this.asto.save(key, new Content.From("one\ntwo\nfour".getBytes(charset))).join();
        new StorageValueAsStream(this.asto, key).process(
            (input, out) -> {
                try {
                    final List<String> list = IOUtils.readLines(input.get(), charset);
                    list.add(2, "three");
                    IOUtils.writeLines(list, "\n", out, charset);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(this.asto.value(key).join()).asciiString().toCompletableFuture().join(),
            new IsEqual<>("one\ntwo\nthree\nfour\n")
        );
    }

    @Test
    void writesNewItem() {
        final Key key = new Key.From("my_test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        final String text = "Hello world!";
        new StorageValueAsStream(this.asto, key).process(
            (input, out) -> {
                MatcherAssert.assertThat(
                    "Input should be absent",
                    input.isPresent(),
                    new IsEqual<>(false)
                );
                try {
                    IOUtils.write(text, out, charset);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "test.txt does not contain text `Hello world!`",
            new PublisherAs(this.asto.value(key).join()).asciiString().toCompletableFuture().join(),
            new IsEqual<>(text)
        );
    }

}

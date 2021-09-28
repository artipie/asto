/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
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
 * Test for {@link StorageValuePipeline}.
 * @since 1.5
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class StorageValuePipelineTest {

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
        new StorageValuePipeline<>(this.asto, key).process(
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
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>("one\ntwo\nthree\nfour\n")
        );
    }

    @Test
    void writesNewItem() {
        final Key key = new Key.From("my_test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        final String text = "Hello world!";
        new StorageValuePipeline<>(this.asto, key).process(
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
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>(text)
        );
    }

    @Test
    void processesExistingItemAndReturnsResult() {
        final Key key = new Key.From("test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        this.asto.save(key, new Content.From("five\nsix\neight".getBytes(charset))).join();
        MatcherAssert.assertThat(
            "Resulting lines count should be 4",
            new StorageValuePipeline<Integer>(this.asto, key).processWithResult(
                (input, out) -> {
                    try {
                        final List<String> list = IOUtils.readLines(input.get(), charset);
                        list.add(2, "seven");
                        IOUtils.writeLines(list, "\n", out, charset);
                        return list.size();
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                }
            ).toCompletableFuture().join(),
            new IsEqual<>(4)
        );
        MatcherAssert.assertThat(
            "Storage item was not updated",
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>("five\nsix\nseven\neight\n")
        );
    }

    @Test
    void writesNewItemAndReturnsResult() {
        final Key key = new Key.From("my_test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        final String text = "Have a food time!";
        MatcherAssert.assertThat(
            new StorageValuePipeline<>(this.asto, key).processWithResult(
                (input, out) -> {
                    MatcherAssert.assertThat(
                        "Input should be absent",
                        input.isPresent(),
                        new IsEqual<>(false)
                    );
                    try {
                        IOUtils.write(text, out, charset);
                        return text.getBytes(charset).length;
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                }
            ).toCompletableFuture().join(),
            new IsEqual<>(17)
        );
        MatcherAssert.assertThat(
            "test.txt does not contain text `Have a food time!`",
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>(text)
        );
    }

}

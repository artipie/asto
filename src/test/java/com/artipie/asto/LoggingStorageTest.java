/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoggingStorage}.
 *
 * @since 0.20.4
 * @todo #186:30min Test that operations are properly logged in LoggingStorage.
 *  We are currently testing that `LoggingStorage` preserves results provided
 *  by `Storage`. We now want to introduce tests that should check that
 *  operation results and parameters are properly logged.
 */
final class LoggingStorageTest {

    @Test
    void retrievesKeyExistingInOriginalStorage() throws Exception {
        final InMemoryStorage memsto = new InMemoryStorage();
        final Key key = new Key.From("repository");
        final Content content = new Content.From(
            "My blog on coding.".getBytes(StandardCharsets.UTF_8)
        );
        memsto.save(key, content).get();
        MatcherAssert.assertThat(
            new LoggingStorage(memsto).exists(key).get(),
            new IsEqual<>(true)
        );
    }

    @Test
    void readsTheSize() throws Exception {
        final InMemoryStorage memsto = new InMemoryStorage();
        final Key key = new Key.From("withSize");
        memsto.save(
            key,
            new Content.From(new byte[]{0x00, 0x00, 0x00})
        ).get();
        MatcherAssert.assertThat(
            new LoggingStorage(memsto).size(key).get(),
            // @checkstyle MagicNumberCheck (1 line)
            new IsEqual<>(3L)
        );
    }

    @Test
    void movesContent() throws Exception {
        final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        final InMemoryStorage memsto = new InMemoryStorage();
        final Key source = new Key.From("from");
        memsto.save(source, new Content.From(data)).get();
        final Key destination = new Key.From("to");
        final LoggingStorage logsto = new LoggingStorage(memsto);
        logsto.move(source, destination).get();
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(
                    logsto.value(destination).get()
                ).single().blockingGet(),
                true
            ).bytes(),
            new IsEqual<>(data)
        );
    }

    @Test
    void savesAndLoads() throws Exception {
        final LoggingStorage storage = new LoggingStorage(new InMemoryStorage());
        final Key key = new Key.From("url");
        final byte[] content = "https://www.artipie.com"
            .getBytes(StandardCharsets.UTF_8);
        storage.save(key, new Content.From(content)).get();
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(storage.value(key).get()).single().blockingGet(),
                true
            ).bytes(),
            new IsEqual<>(content)
        );
    }

    @Test
    void saveOverwrites() throws Exception {
        final byte[] original = "1".getBytes(StandardCharsets.UTF_8);
        final byte[] updated = "2".getBytes(StandardCharsets.UTF_8);
        final InMemoryStorage memsto = new InMemoryStorage();
        final Key key = new Key.From("foo");
        memsto.save(key, new Content.From(original)).get();
        final LoggingStorage logsto = new LoggingStorage(memsto);
        logsto.save(key, new Content.From(updated)).get();
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(logsto.value(key).get()).single().blockingGet(),
                true
            ).bytes(),
            new IsEqual<>(updated)
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
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

    /**
     * Memory storage used in tests.
     */
    private InMemoryStorage memsto;

    @BeforeEach
    void setUp() {
        this.memsto = new InMemoryStorage();
    }

    @Test
    void retrievesKeyExistingInOriginalStorage() {
        final Key key = new Key.From("repository");
        final Content content = new Content.From(
            "My blog on coding.".getBytes(StandardCharsets.UTF_8)
        );
        this.memsto.save(key, content).join();
        MatcherAssert.assertThat(
            new LoggingStorage(this.memsto).exists(key).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void readsTheSize() {
        final Key key = new Key.From("withSize");
        this.memsto.save(
            key,
            new Content.From(new byte[]{0x00, 0x00, 0x00})
        ).join();
        MatcherAssert.assertThat(
            new LoggingStorage(this.memsto).size(key).join(),
            // @checkstyle MagicNumberCheck (1 line)
            new IsEqual<>(3L)
        );
    }

    @Test
    void movesContent() {
        final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        final Key source = new Key.From("from");
        this.memsto.save(source, new Content.From(data)).join();
        final Key destination = new Key.From("to");
        final LoggingStorage logsto = new LoggingStorage(this.memsto);
        logsto.move(source, destination).join();
        MatcherAssert.assertThat(
            new BlockingStorage(logsto).value(destination),
            new IsEqual<>(data)
        );
    }

    @Test
    void savesAndLoads() {
        final LoggingStorage storage = new LoggingStorage(this.memsto);
        final Key key = new Key.From("url");
        final byte[] content = "https://www.artipie.com"
            .getBytes(StandardCharsets.UTF_8);
        storage.save(key, new Content.From(content)).join();
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            new IsEqual<>(content)
        );
    }

    @Test
    void saveOverwrites() {
        final byte[] original = "1".getBytes(StandardCharsets.UTF_8);
        final byte[] updated = "2".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("foo");
        this.memsto.save(key, new Content.From(original)).join();
        final LoggingStorage logsto = new LoggingStorage(this.memsto);
        logsto.save(key, new Content.From(updated)).join();
        MatcherAssert.assertThat(
            new BlockingStorage(logsto).value(key),
            new IsEqual<>(updated)
        );
    }
}

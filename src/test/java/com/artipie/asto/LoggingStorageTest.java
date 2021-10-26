/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import com.artipie.asto.blocking.BlockingStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoggingStorage}.
 *
 * @since 0.20.4
 * @todo #87:60min Add tests for LoggingStorage.
 *  LoggingStorage class lacks tests coverage.
 *  At very base level the tests should check that `LoggingStorage` preserves results
 *  provided by `Storage`. In very best case the tests should check that operation results
 *  and parameters are properly logged.
 */
final class LoggingStorageTest {

    @Test
    void retrievesKeyExistingInOriginalStorage()
        throws ExecutionException, InterruptedException {
        final FakeStorage storage = new FakeStorage();
        final Key key = new Key.From("repository");
        final Content content = new Content.From(
            "My blog on coding.".getBytes(StandardCharsets.UTF_8)
        );
        storage.save(key, content);
        MatcherAssert.assertThat(
            new LoggingStorage(storage).exists(key).get(),
            new IsEqual<>(true)
        );
    }

    @Test
    void readsTheSize() throws Exception {
        final FakeStorage fksto = new FakeStorage();
        final Key key = new Key.From("withSize");
        fksto.save(key, new Content.From(new byte[]{0x00, 0x00, 0x00}));
        MatcherAssert.assertThat(
            new LoggingStorage(fksto).size(key).get(),
            // @checkstyle MagicNumberCheck (1 line)
            new IsEqual<>(3L)
        );
    }

    @Test
    void movesContent() throws Exception {
        final Content data = new Content.From(
            "data".getBytes(StandardCharsets.UTF_8)
        );
        final FakeStorage fksto = new FakeStorage();
        final Key source = new Key.From("from");
        fksto.save(source, data);
        final Key destination = new Key.From("to");
        final LoggingStorage logsto = new LoggingStorage(fksto);
        logsto.move(source, destination);
        MatcherAssert.assertThat(
            logsto.value(destination).get(),
            new IsEqual<>(data)
        );
    }

    @Test
    void savesAndLoads() throws Exception {
        final LoggingStorage storage = new LoggingStorage(new FakeStorage());
        final Key key = new Key.From("repository");
        final Content content = new Content.From(
            "My blog on coding.".getBytes(StandardCharsets.UTF_8)
        );
        storage.save(key, content);
        MatcherAssert.assertThat(
            storage.value(key).get(),
            new IsEqual<>(content)
        );
    }

    @Test
    void saveOverwrites() throws Exception {
        final Content original = new Content.From(
            "1".getBytes(StandardCharsets.UTF_8)
        );
        final Content updated = new Content.From(
            "2".getBytes(StandardCharsets.UTF_8)
        );
        final FakeStorage fksto = new FakeStorage();
        final Key key = new Key.From("foo");
        fksto.save(key, original);
        final LoggingStorage logsto = new LoggingStorage(fksto);
        logsto.save(key, updated);
        MatcherAssert.assertThat(
            logsto.value(key).get(),
            new IsEqual<>(updated)
        );
    }
}

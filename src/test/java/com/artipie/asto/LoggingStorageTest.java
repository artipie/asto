/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoggingStorage}.
 *
 * @since 0.20.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@Disabled
final class LoggingStorageTest {

    /**
     * Memory storage used in tests.
     */
    private InMemoryStorage memsto;

    /**
     * Log writer.
     */
    private StringWriter writer;

    @BeforeEach
    void setUp() {
        this.memsto = new InMemoryStorage();
        this.writer = new StringWriter();
        Logger.getLogger(this.memsto.getClass())
            .addAppender(new WriterAppender(new PatternLayout("%m"), this.writer));
    }

    @Test
    void retrievesKeyExistingInOriginalStorage() {
        final Key key = new Key.From("repository");
        final Content content = new Content.From(
            "My blog on coding.".getBytes(StandardCharsets.UTF_8)
        );
        this.memsto.save(key, content).join();
        MatcherAssert.assertThat(
            "Should retrieve key existing in original storage",
            new LoggingStorage(this.memsto).exists(key).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Should log after checking existence",
            this.writer.toString(),
            new IsEqual<>(String.format("Exists '%s': true", key))
        );
    }

    // @checkstyle MissingDeprecatedCheck (5 lines)
    @Test
    @Deprecated
    void readsSize() {
        final Key key = new Key.From("withSize");
        final byte[] data = new byte[]{0x00, 0x00, 0x00};
        final long dlg = data.length;
        this.memsto.save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Should read the size",
            new LoggingStorage(this.memsto).size(key).join(),
            new IsEqual<>(dlg)
        );
        MatcherAssert.assertThat(
            "Should log after reading size",
            this.writer.toString(),
            new IsEqual<>(String.format("Size '%s': %s", key, dlg))
        );
    }

    @Test
    void savesContent() {
        final byte[] data = "01201".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("binary-key");
        new LoggingStorage(this.memsto).save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Should save content",
            new BlockingStorage(this.memsto).value(key),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Should log after saving content",
            this.writer.toString(),
            new IsEqual<>(String.format("Save '%s': %s", key, Optional.of(data.length)))
        );
    }

    @Test
    void loadsContent() {
        final Key key = new Key.From("url");
        final byte[] data = "https://www.artipie.com"
            .getBytes(StandardCharsets.UTF_8);
        this.memsto.save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Should load content",
            new BlockingStorage(
                new LoggingStorage(this.memsto)
            ).value(key),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Should log after loading content",
            this.writer.toString(),
            new IsEqual<>(
                String.format("Value '%s': %s", key, Optional.of(data.length))
            )
        );
    }

    @Test
    void listsItems() {
        final Key prefix = new Key.From("pref");
        final Key one = new Key.From(prefix, "one");
        final Key two = new Key.From(prefix, "two");
        this.memsto.save(one, Content.EMPTY).join();
        this.memsto.save(two, Content.EMPTY).join();
        final Collection<Key> keys =
            new LoggingStorage(this.memsto).list(prefix).join();
        MatcherAssert.assertThat(
            "Should list items",
            keys,
            Matchers.hasItems(one, two)
        );
        MatcherAssert.assertThat(
            "Should log after listing items",
            this.writer.toString(),
            new IsEqual<>(String.format("List '%s': %s", prefix, keys.size()))
        );
    }

    @Test
    void movesContent() {
        final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        final Key source = new Key.From("from");
        this.memsto.save(source, new Content.From(data)).join();
        final Key destination = new Key.From("to");
        new LoggingStorage(this.memsto).move(source, destination).join();
        MatcherAssert.assertThat(
            "Should move content",
            new BlockingStorage(this.memsto).value(destination),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Should log after moving content",
            this.writer.toString(),
            new IsEqual<>(String.format("Move '%s' '%s'", source, destination))
        );
    }

    @Test
    void deletesContent() {
        final byte[] data = "my file content".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("filename");
        this.memsto.save(key, new Content.From(data)).join();
        new LoggingStorage(this.memsto).delete(key).join();
        MatcherAssert.assertThat(
            "Should delete content",
            new BlockingStorage(this.memsto).exists(key),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Should log after deleting content",
            this.writer.toString(),
            new IsEqual<>(String.format("Delete '%s'", key))
        );
    }

    @Test
    void retrievesMetadata() {
        final Key key = new Key.From("page");
        final byte[] data = "Wiki content".getBytes(StandardCharsets.UTF_8);
        final long dlg = data.length;
        this.memsto.save(key, new Content.From(data)).join();
        final Meta metadata = new LoggingStorage(this.memsto).metadata(key).join();
        MatcherAssert.assertThat(
            "Should retrieve metadata size",
            metadata.read(Meta.OP_SIZE).get(),
            new IsEqual<>(dlg)
        );
        MatcherAssert.assertThat(
            "Should log after retrieving metadata",
            this.writer.toString(),
            new IsEqual<>(String.format("Metadata '%s': %s", key, metadata))
        );
    }

    @Test
    void shouldRunExclusively() {
        final Key key = new Key.From("key-exc");
        final Function<Storage, CompletionStage<Boolean>> operation =
            sto -> CompletableFuture.completedFuture(true);
        this.memsto.save(key, Content.EMPTY).join();
        final Boolean finished = new LoggingStorage(this.memsto)
            .exclusively(key, operation).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Should run exclusively",
            finished,
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Should log after running exclusively",
            this.writer.toString(),
            new IsEqual<>(String.format("Exclusively for '%s': %s", key, operation))
        );
    }
}

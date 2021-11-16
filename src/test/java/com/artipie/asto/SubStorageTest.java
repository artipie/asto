/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link SubStorage}.
 * @since 1.9
 * @todo #352:30min Continue to add more tests for {@link SubStorage}.
 *  All the methods of the class should be verified, do not forget to
 *  add tests with different prefixes, including {@link Key#ROOT} as prefix.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SubStorageTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(strings = {"pref", "composite/prefix"})
    void listsItems(final String pref) {
        final Key prefix = new Key.From(pref);
        this.asto.save(new Key.From(prefix, "one"), Content.EMPTY).join();
        this.asto.save(new Key.From(prefix, "one", "two"), Content.EMPTY).join();
        this.asto.save(new Key.From(prefix, "one", "two", "three"), Content.EMPTY).join();
        this.asto.save(new Key.From(prefix, "another"), Content.EMPTY).join();
        this.asto.save(new Key.From("no_prefix"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Lists items with prefix by ROOT key",
            new SubStorage(prefix, this.asto).list(Key.ROOT).join(),
            Matchers.hasItems(
                new Key.From("one"),
                new Key.From("one/two"),
                new Key.From("one/two/three"),
                new Key.From("another")
            )
        );
        MatcherAssert.assertThat(
            "Lists item with prefix by `one/two` key",
            new SubStorage(prefix, this.asto).list(new Key.From("one/two")).join(),
            Matchers.hasItems(
                new Key.From("one/two"),
                new Key.From("one/two/three")
            )
        );
        MatcherAssert.assertThat(
            "Lists item with ROOT prefix by ROOT key",
            new SubStorage(Key.ROOT, this.asto).list(Key.ROOT).join(),
            new IsEqual<>(this.asto.list(Key.ROOT).join())
        );
        MatcherAssert.assertThat(
            "Lists item with ROOT prefix by `one` key",
            new SubStorage(Key.ROOT, this.asto).list(new Key.From("one")).join(),
            new IsEqual<>(this.asto.list(new Key.From("one")).join())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-project", "com/example"})
    void returnsValue(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "some data".getBytes(StandardCharsets.UTF_8);
        this.asto.save(new Key.From(prefix, "package"), new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Returns storage item with prefix",
            new BlockingStorage(new SubStorage(prefix, this.asto)).value(new Key.From("package")),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Returns storage item with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .value(new Key.From(prefix, "package")),
            new IsEqual<>(data)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "sub/dir"})
    void checksExistence(final String pref) {
        final Key prefix = new Key.From(pref);
        this.asto.save(new Key.From(prefix, "any.txt"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Returns true with prefix when item exists",
            new SubStorage(prefix, this.asto).exists(new Key.From("any.txt")).join()
        );
        MatcherAssert.assertThat(
            "Returns true with ROOT prefix when item exists",
            new SubStorage(Key.ROOT, this.asto).exists(new Key.From(prefix, "any.txt")).join()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-project", "com/example"})
    void savesContent(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "some data".getBytes(StandardCharsets.UTF_8);
        final SubStorage substo = new SubStorage(prefix, this.asto);
        substo.save(new Key.From("package"), new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Returns storage item with prefix",
            new BlockingStorage(this.asto).value(new Key.From(prefix, "package")),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Returns storage item with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .value(new Key.From(prefix, "package")),
            new IsEqual<>(data)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-project", "com/example"})
    void movesContent(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "source".getBytes(StandardCharsets.UTF_8);
        final Key source = new Key.From("src");
        final Key destination = new Key.From("dest");
        this.asto.save(new Key.From(prefix, source), new Content.From(data)).join();
        this.asto.save(
            new Key.From(prefix, destination),
            new Content.From("destination".getBytes(StandardCharsets.UTF_8))
        ).join();
        final SubStorage substo = new SubStorage(prefix, this.asto);
        substo.move(source, destination).join();
        MatcherAssert.assertThat(
            "Moves key value with prefix",
            new BlockingStorage(this.asto).value(new Key.From(prefix, destination)),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Moves key value with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .value(new Key.From(prefix, destination)),
            new IsEqual<>(data)
        );
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @ValueSource(strings = {"url", "sub/url"})
    void readsSize(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "012004407".getBytes(StandardCharsets.UTF_8);
        final Long datalgt = Long.valueOf(data.length);
        final Key keyres = new Key.From("resource");
        this.asto.save(new Key.From(prefix, keyres), new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Gets value size with prefix",
            new BlockingStorage(new SubStorage(prefix, this.asto)).size(keyres),
            new IsEqual<>(datalgt)
        );
        MatcherAssert.assertThat(
            "Gets value size with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .size(new Key.From(prefix, keyres)),
            new IsEqual<>(datalgt)
        );
    }
}

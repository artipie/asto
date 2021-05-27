/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.bench;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import java.util.Map;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link BenchmarkStorage}.
 * @since 1.1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class BenchmarkStorageTest {
    @Test
    void obtainsValueFromBackendIfAbsenceInLocal() {
        final Key key = new Key.From("somekey");
        final byte[] data = "some data".getBytes();
        final Map<String, byte[]> backdata = new MapOf<>(new MapEntry<>(key.string(), data));
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        MatcherAssert.assertThat(
            this.valueFrom(bench, key),
            new IsEqual<>(data)
        );
    }

    @Test
    void obtainsValueFromLocalWithEmptyBackend() {
        final Key key = new Key.From("somekey");
        final byte[] data = "some data".getBytes();
        final Map<Key, byte[]> lcldata = new MapOf<>(new MapEntry<>(key, data));
        final BenchmarkStorage bench = new BenchmarkStorage(new InMemoryStorage(), lcldata);
        MatcherAssert.assertThat(
            this.valueFrom(bench, key),
            new IsEqual<>(data)
        );
    }

    @Test
    void obtainsValueFromLocalWhenInLocalAndBackedIsPresent() {
        final Key key = new Key.From("somekey");
        final byte[] lcl = "some local data".getBytes();
        final byte[] back = "some backend data".getBytes();
        final Map<Key, byte[]> lcldata = new MapOf<>(new MapEntry<>(key, lcl));
        final Map<String, byte[]> backdata = new MapOf<>(new MapEntry<>(key.string(), back));
        final BenchmarkStorage bench = new BenchmarkStorage(
            new InMemoryStorage(backdata), lcldata
        );
        MatcherAssert.assertThat(
            this.valueFrom(bench, key),
            new IsEqual<>(lcl)
        );
    }

    @Test
    void savesOnlyInLocal() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key key = new Key.From("some key");
        final byte[] data = "should save in local".getBytes();
        bench.save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Value was not saved in local storage",
            this.valueFrom(bench, key),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Value was saved in backend storage",
            memory.exists(key).join(),
            new IsEqual<>(false)
        );
    }

    private byte[] valueFrom(final BenchmarkStorage bench, final Key key) {
        return new PublisherAs(bench.value(key).join())
            .bytes()
            .toCompletableFuture().join();
    }
}

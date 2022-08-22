/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.blocking;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BlockingStorage}.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class BlockingStorageTest {

    /**
     * Original storage.
     */
    private Storage original;

    /**
     * BlockingStorage being tested.
     */
    private BlockingStorage blocking;

    @BeforeEach
    void setUp() {
        this.original = new InMemoryStorage();
        this.blocking = new BlockingStorage(this.original);
    }

    @Test
    void shouldExistWhenKeyIsSavedToOriginalStorage() {
        final Key key = new Key.From("test_key_1");
        this.original.save(key, new Content.From("some data1".getBytes())).join();
        MatcherAssert.assertThat(
            this.blocking.exists(key),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldListKeysFromOriginalStorageInOrder() {
        final Content content = new Content.From("some data for test".getBytes());
        this.original.save(new Key.From("1"), content).join();
        this.original.save(new Key.From("a", "b", "c", "1"), content).join();
        this.original.save(new Key.From("a", "b", "2"), content).join();
        this.original.save(new Key.From("a", "z"), content).join();
        this.original.save(new Key.From("z"), content).join();
        final Collection<String> keys = this.blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
        );
    }

    @Test
    void shouldExistInOriginalWhenKeyIsSavedByBlocking() throws Exception {
        final Key key = new Key.From("test_key_2");
        this.blocking.save(key, "test data2".getBytes());
        MatcherAssert.assertThat(
            this.original.exists(key).get(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldMoveInOriginalWhenValueIsMovedByBlocking() {
        final byte[] data = "source".getBytes();
        final Key source = new Key.From("shouldMove-source");
        final Key destination = new Key.From("shouldMove-destination");
        this.original.save(source, new Content.From(data)).join();
        this.blocking.move(source, destination);
        MatcherAssert.assertThat(
            new PublisherAs(
                this.original.value(destination).join()
            ).bytes().toCompletableFuture().join(),
            Matchers.equalTo(data)
        );
    }

    @Test
    void shouldDeleteInOriginalWhenKeyIsDeletedByBlocking() throws Exception {
        final Key key = new Key.From("test_key_6");
        this.original.save(key, Content.EMPTY).join();
        this.blocking.delete(key);
        MatcherAssert.assertThat(
            this.original.exists(key).get(),
            new IsEqual<>(false)
        );
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldReadSize() {
        final Key key = new Key.From("hello_world_url");
        final String page = "<html><h>Hello world</h></html>";
        this.original.save(
            key,
            new Content.From(
                page.getBytes(StandardCharsets.UTF_8)
            )
        ).join();
        MatcherAssert.assertThat(
            this.blocking.size(key),
            new IsEqual<>((long) page.length())
        );
    }

    @Test
    void shouldDeleteAllItemsWithKeyPrefix() {
        final Key prefix = new Key.From("root1");
        this.original.save(new Key.From(prefix, "r1a"), Content.EMPTY).join();
        this.original.save(new Key.From(prefix, "r1b"), Content.EMPTY).join();
        this.original.save(new Key.From("root2", "r2a"), Content.EMPTY).join();
        this.original.save(new Key.From("root3"), Content.EMPTY).join();
        this.blocking.deleteAll(prefix);
        MatcherAssert.assertThat(
            "Original should not have items with key prefix",
            this.original.list(prefix).join().size(),
            new IsEqual<>(0)
        );
        MatcherAssert.assertThat(
            "Original should list other items",
            this.original.list(Key.ROOT).join(),
            Matchers.hasItems(
                new Key.From("root2", "r2a"),
                new Key.From("root3")
            )
        );
    }

    @Test
    void shouldDeleteAllItemsWithRootKey() {
        final Key prefix = new Key.From("dir1");
        this.original.save(new Key.From(prefix, "file1"), Content.EMPTY).join();
        this.original.save(new Key.From(prefix, "file2"), Content.EMPTY).join();
        this.original.save(new Key.From("dir2/subdir", "file3"), Content.EMPTY).join();
        this.original.save(new Key.From("file4"), Content.EMPTY).join();
        this.blocking.deleteAll(Key.ROOT);
        MatcherAssert.assertThat(
            "Original should not have any more item",
            this.original.list(Key.ROOT).join().size(),
            new IsEqual<>(0)
        );
    }
}

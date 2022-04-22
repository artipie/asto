/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#deleteAll(Key)}.
 *
 * @since 1.9.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(StorageExtension.class)
@DisabledOnOs(OS.WINDOWS)
public final class StorageDeleteAllTest {

    @TestTemplate
    void shouldDeleteAllItemsWithKeyPrefix(final Storage storage) {
        final Key prefix = new Key.From("p1");
        storage.save(new Key.From(prefix, "one"), Content.EMPTY).join();
        storage.save(new Key.From(prefix, "two"), Content.EMPTY).join();
        storage.save(new Key.From("p2", "three"), Content.EMPTY).join();
        storage.save(new Key.From("four"), Content.EMPTY).join();
        final BlockingStorage blocking = new BlockingStorage(storage);
        blocking.deleteAll(prefix);
        MatcherAssert.assertThat(
            "Should not have items with key prefix",
            blocking.list(prefix).size(),
            new IsEqual<>(0)
        );
        MatcherAssert.assertThat(
            "Should list other items",
            blocking.list(Key.ROOT),
            Matchers.hasItems(
                new Key.From("p2", "three"),
                new Key.From("four")
            )
        );
        blocking.deleteAll(Key.ROOT);
        MatcherAssert.assertThat(
            "Should remove all items",
            blocking.list(Key.ROOT).size(),
            new IsEqual<>(0)
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#deleteAll(Key)}.
 *
 * @since 1.9.1
 * @todo #348:30min Fix error when trying to save data with prefixed Key.
 *  Test below goes in error when we try to save data in {@link com.artipie.asto.fs.FileStorage}
 *  and {@link com.artipie.asto.fs.VertxFileStorage} with prefixed key, after saving data with
 *  key prefix. Please, fix it and enable this test.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(StorageExtension.class)
public final class StorageDeleteAllTest {

    @Disabled
    @TestTemplate
    void shouldDeleteAllItemsWithKeyPrefix(final Storage storage) {
        final Key prefix = new Key.From("one");
        storage.save(prefix, Content.EMPTY).join();
        storage.save(new Key.From(prefix, "two"), Content.EMPTY).join();
        storage.save(new Key.From(prefix, "two", "three"), Content.EMPTY).join();
        final BlockingStorage blocking = new BlockingStorage(storage);
        blocking.deleteAll(prefix);
        MatcherAssert.assertThat(
            blocking.list(prefix).size(),
            new IsEqual<>(0)
        );
    }
}

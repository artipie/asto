/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#delete(Key)}.
 *
 * @since 0.14
 */
@ExtendWith(StorageExtension.class)
public final class StorageDeleteTest {

    @TestTemplate
    void shouldDeleteValue(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldDeleteValue");
        final byte[] data = "data".getBytes();
        final BlockingStorage blocking = new BlockingStorage(storage);
        blocking.save(key, data);
        blocking.delete(key);
        MatcherAssert.assertThat(
            storage.exists(key).get(),
            new IsEqual<>(false)
        );
    }

    @TestTemplate
    void shouldFailToDeleteNotExistingValue(final Storage storage) {
        final Key key = new Key.From("shouldFailToDeleteNotExistingValue");
        Assertions.assertThrows(Exception.class, () -> storage.delete(key).get());
    }

    @TestTemplate
    void shouldFailToDeleteParentOfSavedKey(final Storage storage) throws Exception {
        final Key parent = new Key.From("shouldFailToDeleteParentOfSavedKey");
        final Key key = new Key.From(parent, "child");
        final byte[] content = "content".getBytes();
        new BlockingStorage(storage).save(key, content);
        Assertions.assertThrows(Exception.class, () -> storage.delete(parent).get());
    }
}

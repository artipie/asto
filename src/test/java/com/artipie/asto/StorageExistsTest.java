/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#exists(Key)}.
 *
 * @since 0.14
 */
@ExtendWith(StorageExtension.class)
@DisabledOnOs(OS.WINDOWS)
public final class StorageExistsTest {

    @TestTemplate
    void shouldExistForSavedKey(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldExistForSavedKey");
        final byte[] data = "some data".getBytes();
        new BlockingStorage(storage).save(key, data);
        MatcherAssert.assertThat(
            storage.exists(key).get(),
            new IsEqual<>(true)
        );
    }

    @TestTemplate
    void shouldNotExistForUnknownKey(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldNotExistForUnknownKey");
        MatcherAssert.assertThat(
            storage.exists(key).get(),
            new IsEqual<>(false)
        );
    }

    @TestTemplate
    void shouldNotExistForParentOfSavedKey(final Storage storage) throws Exception {
        final Key parent = new Key.From("shouldNotExistForParentOfSavedKey");
        final Key key = new Key.From(parent, "child");
        final byte[] data = "content".getBytes();
        new BlockingStorage(storage).save(key, data);
        MatcherAssert.assertThat(
            storage.exists(parent).get(),
            new IsEqual<>(false)
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#size(Key)}.
 *
 * @since 0.17
 */
@ExtendWith(StorageExtension.class)
public final class StorageSizeTest {

    @TestTemplate
    void shouldGetSizeSave(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final byte[] data = "0123456789".getBytes();
        final Key key = new Key.From("shouldGetSizeSave");
        blocking.save(key, data);
        MatcherAssert.assertThat(blocking.size(key), new IsEqual<>((long) data.length));
    }

    @TestTemplate
    void shouldFailToGetSizeOfAbsentValue(final Storage storage) {
        final CompletableFuture<Long> size = storage.size(
            new Key.From("shouldFailToGetSizeOfAbsentValue")
        );
        final Exception exception = Assertions.assertThrows(
            CompletionException.class,
            size::join
        );
        MatcherAssert.assertThat(
            exception.getCause(),
            new IsInstanceOf(ValueNotFoundException.class)
        );
    }
}

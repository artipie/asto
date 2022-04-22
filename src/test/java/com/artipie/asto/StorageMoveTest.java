/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#move(Key, Key)}.
 *
 * @since 0.14
 */
@ExtendWith(StorageExtension.class)
@Timeout(2)
public final class StorageMoveTest {

    @TestTemplate
    void shouldMove(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final byte[] data = "source".getBytes();
        final Key source = new Key.From("shouldMove-source");
        final Key destination = new Key.From("shouldMove-destination");
        blocking.save(source, data);
        blocking.move(source, destination);
        MatcherAssert.assertThat(blocking.value(destination), Matchers.equalTo(data));
    }

    @TestTemplate
    void shouldMoveWhenDestinationExists(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final byte[] data = "source data".getBytes();
        final Key source = new Key.From("shouldMoveWhenDestinationExists-source");
        final Key destination = new Key.From("shouldMoveWhenDestinationExists-destination");
        blocking.save(source, data);
        blocking.save(destination, "destination data".getBytes());
        blocking.move(source, destination);
        MatcherAssert.assertThat(
            blocking.value(destination),
            Matchers.equalTo(data)
        );
    }

    @TestTemplate
    void shouldFailToMoveAbsentValue(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final Key source = new Key.From("shouldFailToMoveAbsentValue-source");
        final Key destination = new Key.From("shouldFailToMoveAbsentValue-destination");
        Assertions.assertThrows(RuntimeException.class, () -> blocking.move(source, destination));
    }
}

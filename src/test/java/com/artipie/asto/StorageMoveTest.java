/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto;

import com.artipie.asto.blocking.StBlocking;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#move(Key, Key)}.
 *
 * @since 0.14
 */
@ExtendWith(StorageExtension.class)
public final class StorageMoveTest {

    @TestTemplate
    void shouldMove(final Storage storage) {
        final StBlocking blocking = new StBlocking(storage);
        final byte[] data = "source".getBytes();
        final Key source = new Key.From("shouldMove-source");
        final Key destination = new Key.From("shouldMove-destination");
        blocking.save(source, data);
        blocking.move(source, destination);
        MatcherAssert.assertThat(blocking.value(destination), Matchers.equalTo(data));
    }

    @TestTemplate
    void shouldMoveWhenDestinationExists(final Storage storage) {
        final StBlocking blocking = new StBlocking(storage);
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
    void shouldFailToMoveAbsentValue(final Storage storage) {
        final StBlocking blocking = new StBlocking(storage);
        final Key source = new Key.From("shouldFailToMoveAbsentValue-source");
        final Key destination = new Key.From("shouldFailToMoveAbsentValue-destination");
        Assertions.assertThrows(RuntimeException.class, () -> blocking.move(source, destination));
    }
}

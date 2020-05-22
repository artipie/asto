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
    void shouldFailToDeleteParentOfSavedKey(final Storage storage) {
        final Key parent = new Key.From("shouldFailToDeleteParentOfSavedKey");
        final Key key = new Key.From(parent, "child");
        final byte[] content = "content".getBytes();
        new BlockingStorage(storage).save(key, content);
        Assertions.assertThrows(Exception.class, () -> storage.delete(parent).get());
    }
}

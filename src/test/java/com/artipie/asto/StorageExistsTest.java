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
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#exists(Key)}.
 *
 * @since 0.14
 */
@ExtendWith(StorageExtension.class)
public final class StorageExistsTest {

    @TestTemplate
    void shouldExistForSavedKey(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldExistForSavedKey");
        final byte[] data = "some data".getBytes();
        new StBlocking(storage).save(key, data);
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
        new StBlocking(storage).save(key, data);
        MatcherAssert.assertThat(
            storage.exists(parent).get(),
            new IsEqual<>(false)
        );
    }
}

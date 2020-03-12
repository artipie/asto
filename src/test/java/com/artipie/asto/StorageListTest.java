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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Storage#list(Key)}.
 *
 * @since 0.14
 */
public final class StorageListTest {

    @ParameterizedTest
    @ArgumentsSource(StorageArgumentProvider.class)
    void shouldListNoKeysWhenEmpty(final Storage storage) {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final Collection<String> keys = blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(keys, Matchers.empty());
    }

    @ParameterizedTest
    @ArgumentsSource(StorageArgumentProvider.class)
    void shouldListKeysInOrder(final Storage storage) {
        final byte[] data = "some data!".getBytes();
        final BlockingStorage blocking = new BlockingStorage(storage);
        blocking.save(new Key.From("1"), data);
        blocking.save(new Key.From("a", "b", "c", "1"), data);
        blocking.save(new Key.From("a", "b", "2"), data);
        blocking.save(new Key.From("a", "z"), data);
        blocking.save(new Key.From("z"), data);
        final Collection<String> keys = blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            "List of keys should be in order",
            keys,
            Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
        );
    }
}

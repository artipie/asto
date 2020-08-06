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
package com.artipie.asto.test;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link TestResource}.
 * @since 0.24
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestResourceTest {

    @Test
    void readsResourceBytes() {
        MatcherAssert.assertThat(
            new TestResource("test.txt").asBytes(),
            new IsEqual<>("hello world".getBytes())
        );
    }

    @Test
    void readsResourceAsStream() {
        MatcherAssert.assertThat(
            new TestResource("test.txt").asInputStream(),
            new IsNot<>(new IsNull<>())
        );
    }

    @Test
    void addsToStorage() {
        final Storage storage = new InMemoryStorage();
        final String path = "test.txt";
        new TestResource(path).saveTo(storage);
        MatcherAssert.assertThat(
            new PublisherAs(storage.value(new Key.From(path)).join())
                .bytes().toCompletableFuture().join(),
            new IsEqual<>("hello world".getBytes())
        );
    }

    @Test
    void addsToStorageBySpecifiedKey() {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("one");
        new TestResource("test.txt").saveTo(storage, key);
        MatcherAssert.assertThat(
            new PublisherAs(storage.value(key).join()).bytes().toCompletableFuture().join(),
            new IsEqual<>("hello world".getBytes())
        );
    }

}

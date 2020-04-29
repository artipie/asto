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
import com.artipie.asto.memory.InMemoryStorage;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * A test for {@link Copy}.
 * @since 0.19
 * @checkstyle LocalFinalVariableNameCheck (500 lines)
 * @checkstyle StringLiteralsConcatenationCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CopyTest {

    @Test
    public void copyTwoFilesFromOneStorageToAnotherWorksFine()
        throws ExecutionException, InterruptedException {
        final Storage from = new InMemoryStorage();
        final Storage to = new InMemoryStorage();
        final Key akey = new Key.From("a.txt");
        final Key bkey = new Key.From("b.txt");
        final BlockingStorage bfrom = new BlockingStorage(from);
        bfrom.save(akey, "Hello world A".getBytes());
        bfrom.save(bkey, "Hello world B".getBytes());
        new Copy(from, Stream.of(bkey, akey).collect(Collectors.toList())).copy(to).get();
        for (final Key key :  new BlockingStorage(from).list(Key.ROOT)) {
            MatcherAssert.assertThat(
                Arrays.equals(
                    bfrom.value(key),
                    new BlockingStorage(to).value(key)
                ),
                Matchers.is(true)
            );
        }
    }
}

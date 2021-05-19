/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
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
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * A test for {@link Copy}.
 * @since 0.19
 * @checkstyle LocalFinalVariableNameCheck (500 lines)
 */
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
        for (final Key key : new BlockingStorage(from).list(Key.ROOT)) {
            MatcherAssert.assertThat(
                Arrays.equals(
                    bfrom.value(key),
                    new BlockingStorage(to).value(key)
                ),
                Matchers.is(true)
            );
        }
    }

    @Test
    public void copyEverythingFromOneStorageToAnotherWorksFine() {
        final Storage from = new InMemoryStorage();
        final Storage to = new InMemoryStorage();
        final Key akey = new Key.From("a/b/c");
        final Key bkey = new Key.From("foo.bar");
        final BlockingStorage bfrom = new BlockingStorage(from);
        bfrom.save(akey, "one".getBytes());
        bfrom.save(bkey, "two".getBytes());
        new Copy(from).copy(to).join();
        for (final Key key : bfrom.list(Key.ROOT)) {
            MatcherAssert.assertThat(
                new BlockingStorage(to).value(key),
                new IsEqual<>(bfrom.value(key))
            );
        }
    }
}

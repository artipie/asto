/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#list(Key)}.
 *
 * @since 0.14
 */
@ExtendWith(StorageExtension.class)
public final class StorageListTest {

    @TestTemplate
    void shouldListNoKeysWhenEmpty(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final Collection<String> keys = blocking.list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(keys, Matchers.empty());
    }

    @TestTemplate
    void shouldListKeysInOrder(final Storage storage) throws Exception {
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
            keys,
            Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
        );
    }
}

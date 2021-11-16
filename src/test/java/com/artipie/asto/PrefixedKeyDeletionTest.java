/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * A test for {@link PrefixedKeyDeletion}.
 * @since 1.9.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PrefixedKeyDeletionTest {

    @Test
    public void removesPrefixedKeys() {
        final Storage asto = new InMemoryStorage();
        final Key prefix = new Key.From("one");
        asto.save(prefix, Content.EMPTY).join();
        asto.save(new Key.From(prefix, "two"), Content.EMPTY).join();
        asto.save(new Key.From(prefix, "two", "three"), Content.EMPTY).join();
        final Key otherkey = new Key.From("another");
        asto.save(otherkey, Content.EMPTY).join();
        new PrefixedKeyDeletion(asto).remove(prefix).join();
        MatcherAssert.assertThat(
            new BlockingStorage(asto).list(Key.ROOT),
            new IsEqual<>(Arrays.asList(otherkey))
        );
    }
}

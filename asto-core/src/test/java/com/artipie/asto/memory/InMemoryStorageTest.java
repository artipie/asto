/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link InMemoryStorage}.
 *
 * @since 0.18
 */
class InMemoryStorageTest {

    /**
     * Storage being tested.
     */
    private InMemoryStorage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    @Timeout(1)
    void shouldNotBeBlockedByEndlessContent() throws Exception {
        final Key.From key = new Key.From("data");
        this.storage.save(
            key,
            new Content.From(
                ignored -> {
                }
            )
        );
        // @checkstyle MagicNumberCheck (1 line)
        Thread.sleep(100);
        MatcherAssert.assertThat(
            this.storage.exists(key).get(1, TimeUnit.SECONDS),
            new IsEqual<>(false)
        );
    }
}

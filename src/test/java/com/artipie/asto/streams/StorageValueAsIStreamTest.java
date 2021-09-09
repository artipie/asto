/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.misc.UncheckedIOFunc;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link StorageValueAsIStream}.
 * @since 1.4
 */
class StorageValueAsIStreamTest {

    @Test
    void processesItem() {
        final Storage asto = new InMemoryStorage();
        final Key.From key = new Key.From("some_text");
        final Charset charset = StandardCharsets.UTF_8;
        asto.save(key, new Content.From("one\ntwo\nthree".getBytes(charset))).join();
        MatcherAssert.assertThat(
            new StorageValueAsIStream<List<String>>(asto, key).process(
                new UncheckedIOFunc<>(input -> IOUtils.readLines(input, charset))
            ).toCompletableFuture().join(),
            Matchers.contains("one", "two", "three")
        );
    }

}

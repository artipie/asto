/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link KeyPath}.
 *
 * @since 1.11
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
final class KeyPathTest {

    @Test
    void validatesThatPathIsInRootLocation() {
        MatcherAssert.assertThat(
            "Should validate simple relative key",
            new KeyPath(
                "/x/y",
                new Key.From("z")
            ).get().toString(),
            new IsEqual<>("/x/y/z")
        );
        MatcherAssert.assertThat(
            "Should validate key with redundant ..",
            new KeyPath(
                "/a/b/c",
                new Key.From("../c/d")
            ).get().toString(),
            new IsEqual<>("/a/b/c/d")
        );
        MatcherAssert.assertThat(
            "Should validate key with redundant .",
            new KeyPath(
                "/f/g/h",
                new Key.From("./i")
            ).get().toString(),
            new IsEqual<>("/f/g/h/i")
        );
        MatcherAssert.assertThat(
            "Should validate key with root location containing redundant .",
            new KeyPath(
                "/f/g/.././h",
                new Key.From("i")
            ).get().toString(),
            new IsEqual<>("/f/h/i")
        );
    }

    @Test
    void throwsExceptionWhenOutOfRoot() {
        final ArtipieIOException aie = Assertions.assertThrows(
            ArtipieIOException.class,
            () -> new KeyPath("/k/l/m", new Key.From("../n/o")).get()
        );
        MatcherAssert.assertThat(
            "Should throw with exception message",
            ExceptionUtils.getRootCause(aie).getLocalizedMessage(),
            new IsEqual<>("Entry path is out of root location: /k/l/n/o")
        );
    }
}

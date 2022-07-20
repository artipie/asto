/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.fs;

import com.artipie.asto.Meta;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test case for {@link FileMeta}.
 * @since 1.9
 */
final class FileMetaTest {

    @Test
    void readAttrs() {
        final long len = 4;
        final Instant creation = Instant.ofEpochMilli(1);
        final Instant modified = Instant.ofEpochMilli(2);
        final Instant access = Instant.ofEpochMilli(3);
        final BasicFileAttributes attrs = Mockito.mock(BasicFileAttributes.class);
        Mockito.when(attrs.size()).thenReturn(len);
        Mockito.when(attrs.creationTime()).thenReturn(FileTime.from(creation));
        Mockito.when(attrs.lastModifiedTime()).thenReturn(FileTime.from(modified));
        Mockito.when(attrs.lastAccessTime()).thenReturn(FileTime.from(access));
        MatcherAssert.assertThat(
            "size",
            new FileMeta(attrs).read(Meta.OP_SIZE).get(),
            new IsEqual<>(len)
        );
        MatcherAssert.assertThat(
            "created at",
            new FileMeta(attrs).read(Meta.OP_CREATED_AT).get(),
            new IsEqual<>(creation)
        );
        MatcherAssert.assertThat(
            "updated at",
            new FileMeta(attrs).read(Meta.OP_UPDATED_AT).get(),
            new IsEqual<>(modified)
        );
        MatcherAssert.assertThat(
            "accessed at",
            new FileMeta(attrs).read(Meta.OP_ACCESSED_AT).get(),
            new IsEqual<>(access)
        );
    }
}

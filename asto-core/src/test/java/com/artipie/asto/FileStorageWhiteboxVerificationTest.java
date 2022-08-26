/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.StorageWhiteboxVerification;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;

/**
 * File storage verification test.
 *
 * @checkstyle ProtectedMethodInFinalClassCheck (500 lines)
 * @since 1.14.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class FileStorageWhiteboxVerificationTest extends StorageWhiteboxVerification {

    /**
     * Temp test dir.
     */
    private Path temp;

    @Before
    public void setUp() throws Exception {
        this.temp = Files.createTempDirectory("junit");
    }

    @After
    public void tearDown() throws Exception {
        try (Stream<Path> walk = Files.walk(this.temp)) {
            walk.map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Override
    protected Storage newStorage() {
        return new FileStorage(this.temp.resolve("base"));
    }

    @Override
    protected Optional<Storage> newBaseForRootSubStorage() {
        return Optional.of(new FileStorage(this.temp.resolve("root-sub-storage")));
    }

    @Override
    protected Optional<Storage> newBaseForSubStorage() throws Exception {
        return Optional.of(new FileStorage(this.temp.resolve("sub-storage")));
    }
}

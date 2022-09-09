/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.fs.VertxFileStorage;
import com.artipie.asto.test.StorageWhiteboxVerification;
import io.vertx.reactivex.core.Vertx;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;

/**
 * Vertx file storage verification test.
 *
 * @checkstyle ProtectedMethodInFinalClassCheck (500 lines)
 * @since 0.1
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class VertxFileStorageVerificationTest extends StorageWhiteboxVerification {

    /**
     * Temp dir.
     */
    private Path temp;

    /**
     * Vert.x file System.
     */
    private Vertx vertx;

    @Before
    public void setUp() throws Exception {
        this.vertx = Vertx.vertx();
        this.temp = Files.createTempDirectory("vtx_junit");
    }

    @After
    public void tearDown() throws Exception {
        this.vertx.close();
        try (Stream<Path> walk = Files.walk(this.temp)) {
            walk.map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Override
    protected Storage newStorage() throws Exception {
        return new VertxFileStorage(
            this.temp.resolve("base"),
            this.vertx
        );
    }

    @Override
    protected Optional<Storage> newBaseForRootSubStorage() {
        return Optional.of(
            new VertxFileStorage(this.temp.resolve("root-sub-storage"), this.vertx)
        );
    }

    @Override
    protected Optional<Storage> newBaseForSubStorage() {
        return Optional.of(
            new VertxFileStorage(this.temp.resolve("sub-storage"), this.vertx)
        );
    }

}

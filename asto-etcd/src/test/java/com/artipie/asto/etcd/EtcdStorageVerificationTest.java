/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.etcd;

import com.artipie.asto.Storage;
import com.artipie.asto.test.StorageWhiteboxVerification;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.test.EtcdClusterExtension;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * ETCD storage verification test.
 *
 * @checkstyle ProtectedMethodInFinalClassCheck (500 lines)
 * @since 0.1
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
@DisabledOnOs(OS.WINDOWS)
public final class EtcdStorageVerificationTest extends StorageWhiteboxVerification {
    /**
     * Etcd cluster.
     */
    private static EtcdClusterExtension etcd;

    @BeforeClass
    public static void beforeClass() throws Exception {
        EtcdStorageVerificationTest.etcd = new EtcdClusterExtension(
            "test-etcd",
            1,
            false,
            "--data-dir",
            "/data.etcd0"
        );
        EtcdStorageVerificationTest.etcd.beforeAll(null);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EtcdStorageVerificationTest.etcd.afterAll(null);
    }

    @Override
    protected Storage newStorage() throws Exception {
        return new EtcdStorage(
            Client.builder()
                .endpoints(EtcdStorageVerificationTest.etcd.getClientEndpoints())
                .build()
        );
    }

    @Override
    protected Optional<Storage> newBaseForRootSubStorage() {
        return Optional.empty();
    }
}

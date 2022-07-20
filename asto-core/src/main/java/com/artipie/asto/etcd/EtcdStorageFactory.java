/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.etcd;

import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.StorageConfig;
import com.artipie.asto.factory.StorageFactory;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import java.time.Duration;

/**
 * Etcd storage factory.
 * @since 1.13.0
 */
@ArtipieStorageFactory("etcd")
public final class EtcdStorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final StorageConfig cfg) {
        final StorageConfig connection = new StorageConfig.StrictStorageConfig(cfg)
            .config("connection");
        final ClientBuilder builder = Client.builder()
            .endpoints(
                connection.sequence("endpoints").toArray(new String[0])
            );
        final String sto = connection.string("timeout");
        if (sto != null) {
            builder.connectTimeout(Duration.ofMillis(Integer.parseInt(sto)));
        }
        return new EtcdStorage(builder.build());
    }
}

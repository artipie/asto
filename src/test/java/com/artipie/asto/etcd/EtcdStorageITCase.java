/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */

package com.artipie.asto.etcd;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.test.EtcdClusterExtension;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test case for etcd-storage.
 * @since 1.0
 * @todo #306:90min The test is disabled,
 *  the CI can't start ETCD extension which depends on testcontainer with
 *  etcd Docker image. Let's fix this issue on CI and enable the test.
 * @todo #306:90min Add etcd storage to StorageExtension to run all common
 *  tests on EtcdStorage too. It could be not an easy task, since etcd
 *  test depends on etcd clust with at least one node. For this test
 *  it starts using testcontainers and `EtcdCluster` junit extension.
 */
@Disabled
public final class EtcdStorageITCase {

    /**
     * Test cluster.
     */
    @RegisterExtension
    static final EtcdCluster ETCD = new EtcdClusterExtension("test-etcd", 1);

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new EtcdStorage(
            Client.builder().endpoints(ETCD.getClientEndpoints()).build()
        );
    }

    @Test
    void readAndWrite() {
        final Key key = new Key.From("one", "two", "three");
        final byte[] data = "some binary data".getBytes();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        bsto.save(key, "first revision".getBytes());
        bsto.save(key, "second revision".getBytes());
        bsto.save(key, data);
        MatcherAssert.assertThat(bsto.value(key), Matchers.equalTo(data));
    }

    @Test
    void getSize() {
        final Key key = new Key.From("another", "key");
        final byte[] data = "data with size".getBytes();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        bsto.save(key, data);
        MatcherAssert.assertThat(bsto.size(key), Matchers.equalTo((long) data.length));
    }

    @Test
    void checkExist() {
        final Key key = new Key.From("existing", "item");
        final byte[] data = "I exist".getBytes();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        bsto.save(key, data);
        MatcherAssert.assertThat(bsto.exists(key), Matchers.is(true));
    }

    @Test
    void move() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key src = new Key.From("source");
        final Key dst = new Key.From("destination");
        final byte[] data = "data to move".getBytes();
        bsto.save(src, data);
        bsto.move(src, dst);
        MatcherAssert.assertThat("source still exist", bsto.exists(src), new IsEqual<>(false));
        MatcherAssert.assertThat("source was not moved", bsto.value(dst), new IsEqual<>(data));
    }

    @Test
    void delete() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key key = new Key.From("temporary");
        final byte[] data = "data to delete".getBytes();
        bsto.save(key, data);
        bsto.delete(key);
        MatcherAssert.assertThat(bsto.exists(key), new IsEqual<>(false));
    }

    @Test
    void failsIfNothingToDelete() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        final Key key = new Key.From("nothing");
        final CompletionException cex = Assertions.assertThrows(
            CompletionException.class,
            () -> bsto.delete(key)
        );
        MatcherAssert.assertThat(
            cex.getCause().getMessage(),
            Matchers.stringContainsInOrder("Key", key.toString(), "was not found")
        );
    }
}

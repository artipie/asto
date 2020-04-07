/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.asto.google;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * GoogleStorageTest.
 *
 * @since 0.18
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GoogleStorageTest {

    /**
     * Api test host.
     */
    private static final String HOST = "localhost";

    /**
     * Api test port.
     */
    private static final int PORT = 16_969;

    /**
     * Vertx.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Test server.
     */
    private static final VertxTestWebServer SERVER = new VertxTestWebServer(
        GoogleStorageTest.VERTX, GoogleStorageTest.PORT, new GetResponse()
    );

    @BeforeAll
    static void runServer() {
        GoogleStorageTest.SERVER.start();
    }

    @Test
    void shouldGetValue() throws ExecutionException, InterruptedException {
        final Content content = new GoogleStorage(
            GoogleStorageTest.VERTX,
            GoogleStorageTest.HOST,
            GoogleStorageTest.PORT,
            WebClient.create(GoogleStorageTest.VERTX), "bucket"
        ).value(new Key.From("key")).get();
        final ByteBuffer buffer = Flowable.fromPublisher(content).blockingFirst();
        MatcherAssert.assertThat(
            new String(
                new Remaining(buffer).bytes(),
                StandardCharsets.UTF_8
            ), Matchers.equalTo("response")
        );
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop();
    }
}

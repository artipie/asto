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
import com.artipie.asto.Storage;
import com.artipie.asto.Transaction;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.streams.WriteStream;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Storage that holds data in Google storage.
 *
 * @since 0.17
 * @checkstyle ConstantUsageCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GoogleStorage implements Storage {

    /**
     * API GET_REQUEST.
     */
    private static final String GET_REQUEST = "/storage/v1/b/%s/o/%s";

    /**
     * API HOST.
     */
    private static final String HOST = "storage.googleapis.com";

    /**
     * API PORT.
     */
    private static final int PORT = 443;

    /**
     * Vertx context.
     */
    private final Vertx vertx;

    /**
     * Google storage client.
     */
    private final WebClient client;

    /**
     * Bucket name.
     */
    private final String bucket;

    /**
     * Api host.
     */
    private final String host;

    /**
     * Api port.
     */
    private final int port;

    /**
     * Ctor.
     *
     * @param vertx Vertx context
     * @param client Web client
     * @param bucket Bucket name
     */
    public GoogleStorage(final Vertx vertx, final WebClient client, final String bucket) {
        this(vertx, GoogleStorage.HOST, GoogleStorage.PORT, client, bucket);
    }

    /**
     * Ctor.
     *
     * @param vertx Vertx context
     * @param host Api host
     * @param port Api port
     * @param client Web client
     * @param bucket Bucket name
     */
    public GoogleStorage(
        final Vertx vertx,
        final String host,
        final int port,
        final WebClient client,
        final String bucket) {
        this.vertx = vertx;
        this.host = host;
        this.port = port;
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                final ReactiveWriteStream<Buffer> stream = ReactiveWriteStream
                    .writeStream(this.vertx.getDelegate());
                final Flowable<ByteBuffer> flow = Flowable.fromPublisher(stream)
                    .map(buffer -> ByteBuffer.wrap(buffer.getBytes()));
                this.client.get(
                    this.port,
                    this.host,
                    String.format(GoogleStorage.GET_REQUEST, this.bucket, key.string())
                )
                    .as(BodyCodec.pipe(WriteStream.newInstance(stream)))
                    .rxSend().subscribe();
                return new Content.From(flow);
            });
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Transaction> transaction(final List<Key> keys) {
        throw new UnsupportedOperationException();
    }
}

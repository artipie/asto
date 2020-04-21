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

package com.artipie.asto.alibaba;

import com.aliyun.oss.OSS;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.Transaction;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AlibabaStorage.
 *
 * Docs on sdk: https://www.alibabacloud.com/help/doc-detail/32009.htm
 *
 * @since 0.19
 */
public final class AlibabaStorage implements Storage {

    /**
     * OSS client.
     */
    private final OSS client;

    /**
     * Bucket name.
     */
    private final String bucket;

    /**
     * Ctor.
     *
     * @param client OSS Client
     * @param bucket Bucket name
     */
    public AlibabaStorage(final OSS client, final String bucket) {
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
        return new Concatenation(content).single()
            .map(Remaining::new)
            .map(Remaining::bytes)
            .map(ByteArrayInputStream::new)
            .to(SingleInterop.get())
            .thenApply(bytes -> this.client.putObject(this.bucket, key.string(), bytes))
            .<Void>thenApply(o -> null)
            .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        throw new UnsupportedOperationException();
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

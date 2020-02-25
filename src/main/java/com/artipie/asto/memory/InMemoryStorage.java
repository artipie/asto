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
package com.artipie.asto.memory;

import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.Transaction;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Simple implementation of Storage that holds all data in memory.
 *
 * @since 0.14
 */
public final class InMemoryStorage implements Storage {

    /**
     * Values stored by key strings.
     */
    private final Map<String, byte[]> data = new HashMap<>();

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Publisher<ByteBuffer> content) {
        return CompletableFuture.runAsync(
            () -> this.data.put(
                key.string(),
                Flowable.fromPublisher(content)
                    .toList()
                    .blockingGet()
                    .stream()
                    .reduce(
                        (left, right) -> {
                            final ByteBuffer concat = ByteBuffer.allocate(
                                left.remaining() + right.remaining()
                            ).put(left).put(right);
                            concat.flip();
                            return concat;
                        }
                    )
                    .map(buf -> new Remaining(buf).bytes())
                    .orElse(new byte[0])
            )
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Publisher<ByteBuffer>> value(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                final byte[] bytes = this.data.get(key.string());
                if (bytes == null) {
                    throw new IllegalArgumentException(
                        String.format("No value for key: %s", key.string())
                    );
                }
                return Flowable.fromArray(ByteBuffer.wrap(bytes));
            }
        );
    }

    @Override
    public CompletableFuture<Transaction> transaction(final List<Key> keys) {
        throw new UnsupportedOperationException();
    }
}

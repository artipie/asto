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
package com.artipie.asto.fs;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.Transaction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Transaction on {@link FileStorage}.
 *
 * @since 0.10
 */
public final class FileSystemTransaction implements Transaction {

    /**
     * The parent storage.
     */
    private final Storage parent;

    /**
     * Ctor.
     *
     * @param parent The parent
     */
    public FileSystemTransaction(
        final Storage parent) {
        this.parent = parent;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return this.parent.exists(key);
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return this.parent.list(prefix);
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return this.parent.move(source, destination);
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.parent.save(key, content);
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        return this.parent.size(key);
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return this.parent.value(key);
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.parent.delete(key);
    }

    @Override
    public CompletableFuture<Transaction> transaction(
        // @checkstyle HiddenFieldCheck (1 line)
        final List<Key> keys) {
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public CompletableFuture<Void> commit() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> rollback() {
        return CompletableFuture.completedFuture(null);
    }
}

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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.Transaction;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;
import org.reactivestreams.FlowAdapters;

/**
 * Simple storage, in files.
 *
 * @since 0.1
 */
public final class FileStorage implements Storage {

    /**
     * Where we keep the data.
     */
    private final Path dir;

    /**
     * The Vert.x.
     */
    private final FileSystem fls;

    /**
     * Ctor.
     *
     * @param path The path to the dir.
     */
    public FileStorage(final Path path) {
        this(path, Vertx.vertx().fileSystem());
    }

    /**
     * Ctor.
     *
     * @param path The path to the dir
     * @param fls The file system
     */
    public FileStorage(final Path path, final FileSystem fls) {
        this.dir = path;
        this.fls = fls;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return Single.fromCallable(
            () -> Files.exists(this.path(key))
        ).to(SingleInterop.get()).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return Single.fromCallable(
            () -> {
                final Path path = this.path(prefix);
                final Collection<Key> keys;
                if (Files.exists(path)) {
                    final int dirnamelen = path.toString().length() - prefix.string().length();
                    keys = Files.walk(path)
                        .filter(Files::isRegularFile)
                        .map(Path::toString)
                        .map(p -> p.substring(dirnamelen))
                        .map(
                            s -> s.split(
                                FileSystems.getDefault().getSeparator().replace("\\", "\\\\")
                            )
                        )
                        .map(Key.From::new)
                        .sorted(Comparator.comparing(Key.From::string))
                        .collect(Collectors.toList());
                } else {
                    keys = Collections.emptyList();
                }
                Logger.info(
                    this,
                    "Found %d objects by the prefix \"%s\" in %s by %s: %s",
                    keys.size(), prefix, this.dir, path, keys
                );
                return keys;
            }).to(SingleInterop.get()).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Flow.Publisher<ByteBuffer> content) {
        return Single.fromCallable(
            () -> {
                final Path file = this.path(key);
                file.getParent().toFile().mkdirs();
                return file;
            })
            .flatMapCompletable(
                file -> new RxFile(file, this.fls)
                    .save(Flowable.fromPublisher(FlowAdapters.toPublisher(content)))
            ).to(CompletableInterop.await())
            .<Void>thenApply(o -> null)
            .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return Single.fromCallable(
            () -> {
                final Path dest = this.path(destination);
                dest.getParent().toFile().mkdirs();
                return dest;
            })
            .flatMapCompletable(
                dest -> new RxFile(this.path(source), this.fls).move(dest)
            )
            .to(CompletableInterop.await())
            .<Void>thenApply(file -> null)
            .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Flow.Publisher<ByteBuffer>> value(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> FlowAdapters.toFlowPublisher(
                new RxFile(this.path(key), this.fls).flow()
            )
        );
    }

    @Override
    public CompletableFuture<Transaction> transaction(final List<Key> keys) {
        return CompletableFuture.completedFuture(new FileSystemTransaction(this));
    }

    /**
     * Resolves key to file system path.
     *
     * @param key Key to be resolved to path.
     * @return Path created from key.
     */
    private Path path(final Key key) {
        return Paths.get(this.dir.toString(), key.string());
    }
}

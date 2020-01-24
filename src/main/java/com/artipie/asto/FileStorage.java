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
package com.artipie.asto;

import com.jcabi.log.Logger;
import hu.akarnokd.rxjava3.jdk8interop.CompletableInterop;
import hu.akarnokd.rxjava3.jdk8interop.SingleInterop;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
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
     * Ctor.
     *
     * @param path The path to the dir
     */
    public FileStorage(final Path path) {
        this.dir = path;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return Single.fromCallable(
            () -> Files.exists(Paths.get(this.dir.toString(), key.string()))
        ).to(SingleInterop.get()).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final String prefix) {
        return Single.fromCallable(
            () -> {
                final String separator = FileSystems.getDefault().getSeparator();
                if (!prefix.endsWith(separator)) {
                    throw new IllegalArgumentException(
                        String.format(
                            "The prefix must end with '%s': \"%s\"",
                            prefix,
                            separator
                        )
                    );
                }
                final Path path = Paths.get(this.dir.toString(), prefix);
                final int dirnamelen = path.toString().length() - prefix.length() + 1;
                final Collection<Key> keys = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .map(p -> p.substring(dirnamelen))
                    .map(p -> new Key.From(p))
                    .collect(Collectors.toList());
                Logger.info(
                    this,
                    "Found %d objects by the prefix \"%s\" in %s by %s: %s",
                    keys.size(), prefix, this.dir, path, keys
                );
                return keys;
            }).to(SingleInterop.get()).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Flow.Publisher<Byte> content) {
        return Flowable.fromPublisher(FlowAdapters.toPublisher(content))
            .toList()
            .map(bytes -> new ByteArray(bytes.toArray(new Byte[0])).primitiveBytes())
            .flatMapCompletable(
                bytes ->
                    Completable.fromAction(
                        () -> {
                            final Path target = Paths.get(this.dir.toString(), key.string());
                            target.getParent().toFile().mkdirs();
                            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
                            Logger.info(
                                this,
                                "Saved %d bytes to %s: %s",
                                bytes, key, target
                            );
                        }
                    )
            )
            .to(CompletableInterop.await())
            .<Void>thenApply(o -> null)
            .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Flow.Publisher<Byte>> value(final Key key) {
        final Path source = Paths.get(this.dir.toString(), key.string());
        final Flowable<Byte> result =
            Single.fromCallable(
                () -> {
                    final byte[] bytes = Files.readAllBytes(source);
                    Logger.info(
                        this,
                        "Loaded %d bytes of %s: %s",
                        bytes.length, key, source
                    );
                    return bytes;
                }
            ).flatMapPublisher(
                bytes -> Flowable.fromIterable(Arrays.asList(new ByteArray(bytes).boxedBytes()))
            );
        return CompletableFuture.supplyAsync(() -> FlowAdapters.toFlowPublisher(result));
    }
}

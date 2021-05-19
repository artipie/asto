/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.fs;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Storage;
import com.artipie.asto.UnderLockOperation;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.lock.storage.StorageLock;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cqfn.rio.file.File;

/**
 * Simple storage, in files.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class FileStorage implements Storage {

    /**
     * Where we keep the data.
     */
    private final Path dir;

    /**
     * Ctor.
     * @param path The path to the dir
     * @param nothing Just for compatibility
     * @deprecated Use {@link FileStorage#FileStorage(Path)} ctor instead.
     */
    @Deprecated
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public FileStorage(final Path path, final Object nothing) {
        this(path);
    }

    /**
     * Ctor.
     * @param path The path to the dir
     */
    public FileStorage(final Path path) {
        this.dir = path;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                final Path path = this.path(key);
                return Files.exists(path) && !Files.isDirectory(path);
            }
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return CompletableFuture.supplyAsync(
            () -> {
                final Path path = this.path(prefix);
                final Collection<Key> keys;
                if (Files.exists(path)) {
                    final int dirnamelen;
                    if (Key.ROOT.equals(prefix)) {
                        dirnamelen = path.toString().length() + 1;
                    } else {
                        dirnamelen = path.toString().length() - prefix.string().length();
                    }
                    try {
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
                    } catch (final IOException iex) {
                        throw new ArtipieIOException(iex);
                    }
                } else {
                    keys = Collections.emptyList();
                }
                Logger.info(
                    this,
                    "Found %d objects by the prefix \"%s\" in %s by %s: %s",
                    keys.size(), prefix.string(), this.dir, path, keys
                );
                return keys;
            }
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return CompletableFuture.supplyAsync(
            () -> {
                final Path tmp = Paths.get(
                    this.dir.toString(),
                    String.format("%s.%s.tmp", key.string(), UUID.randomUUID())
                );
                tmp.getParent().toFile().mkdirs();
                return tmp;
            }
        ).thenCompose(
            tmp -> new File(tmp).write(
                new OneTimePublisher<>(content),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).thenCompose(
                nothing -> FileStorage.move(tmp, this.path(key))
            ).handleAsync(
                (nothing, throwable) -> {
                    tmp.toFile().delete();
                    final CompletableFuture<Void> result = new CompletableFuture<>();
                    if (throwable == null) {
                        result.complete(null);
                    } else {
                        result.completeExceptionally(new ArtipieIOException(throwable));
                    }
                    return result;
                }
            ).thenCompose(Function.identity())
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return FileStorage.move(this.path(source), this.path(destination));
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return CompletableFuture.runAsync(
            () -> {
                final Path path = this.path(key);
                if (Files.exists(path) && !Files.isDirectory(path)) {
                    try {
                        Files.delete(path);
                        this.deleteEmptyParts(key.parent());
                    } catch (final IOException iex) {
                        throw new ArtipieIOException(iex);
                    }
                } else {
                    throw new ValueNotFoundException(key);
                }
            }
        );
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.size(this.path(key));
                } catch (final NoSuchFileException nofile) {
                    throw new ValueNotFoundException(key, nofile);
                } catch (final IOException iex) {
                    throw new ArtipieIOException(iex);
                }
            }
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final CompletableFuture<Content> res;
        if (Key.ROOT.equals(key)) {
            res = new CompletableFutureSupport.Failed<Content>(
                new ArtipieIOException("Unable to load from root")
            ).get();
        } else {
            res = this.size(key).thenApply(
                size -> new Content.OneTime(
                    new Content.From(size, new File(this.path(key)).content())
                )
            );
        }
        return res;
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        return new UnderLockOperation<>(new StorageLock(this, key), operation).perform(this);
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

    /**
     * Removes empty key parts (directories).
     * @param key Key
     * @checkstyle NestedIfDepthCheck (20 lines)
     */
    private void deleteEmptyParts(final Optional<Key> key) {
        if (key.isPresent() && !key.get().string().isEmpty()) {
            final Path path = this.path(key.get());
            if (Files.isDirectory(path)) {
                boolean again = false;
                try {
                    try (Stream<Path> files = Files.list(path)) {
                        if (!files.findFirst().isPresent()) {
                            Files.deleteIfExists(path);
                            again = true;
                        }
                    }
                    if (again) {
                        this.deleteEmptyParts(key.get().parent());
                    }
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        }
    }

    /**
     * Moves file from source path to destination.
     *
     * @param source Source path.
     * @param dest Destination path.
     * @return Completion of moving file.
     */
    private static CompletableFuture<Void> move(final Path source, final Path dest) {
        return CompletableFuture.supplyAsync(
            () -> {
                dest.getParent().toFile().mkdirs();
                return dest;
            }
        ).thenAcceptAsync(
            dst -> {
                try {
                    Files.move(source, dst, StandardCopyOption.REPLACE_EXISTING);
                } catch (final IOException iex) {
                    throw new ArtipieIOException(iex);
                }
            }
        );
    }
}

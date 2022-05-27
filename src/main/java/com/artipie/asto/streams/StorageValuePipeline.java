/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.stream.ReactiveInputStream;
import org.cqfn.rio.stream.ReactiveOutputStream;

/**
 * Processes storage value content as optional input stream and
 * saves the result back as output stream.
 *
 * @param <R> Result type
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class StorageValuePipeline<R> {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Storage item key to read from.
     */
    private final Key read;

    /**
     * Storage item key to write to.
     */
    private final Key write;

    /**
     * Ctor.
     *
     * @param asto Abstract storage
     * @param read Storage item key to read from
     * @param write Storage item key to write to
     */
    public StorageValuePipeline(final Storage asto, final Key read, final Key write) {
        this.asto = asto;
        this.read = read;
        this.write = write;
    }

    /**
     * Ctor.
     *
     * @param asto Abstract storage
     * @param key Item key
     */
    public StorageValuePipeline(final Storage asto, final Key key) {
        this(asto, key, key);
    }

    /**
     * Process storage item and save it back.
     *
     * @param action Action to perform with storage content if exists and write back as
     *  output stream.
     * @return Completion action
     * @throws ArtipieIOException On Error
     */
    public CompletionStage<Void> process(
        final BiConsumer<Optional<InputStream>, OutputStream> action
    ) {
        return this.processWithResult(
            (opt, input) -> {
                action.accept(opt, input);
                return null;
            }
        ).thenAccept(
            nothing -> {
            }
        );
    }

    /**
     * Process storage item, save it back and return some result.
     *
     * @param action Action to perform with storage content if exists and write back as
     *  output stream.
     * @return Completion action with the result
     * @throws ArtipieIOException On Error
     */
    public CompletionStage<R> processWithResult(
        final BiFunction<Optional<InputStream>, OutputStream, R> action
    ) {
        return this.asto.exists(this.read)
            .thenCompose(
                exists -> {
                    try {
                        final Optional<InputStream> inpfrom;
                        if (exists) {
                            final PipedOutputStream outfrom = new PipedOutputStream();
                            inpfrom = Optional.of(new PipedInputStream(outfrom));
                            this.asto.value(this.read)
                                .thenCompose(
                                    content -> new ReactiveOutputStream(outfrom)
                                        .write(content, WriteGreed.SYSTEM)
                                        .toCompletableFuture()
                                ).handle(new FutureHandler<>(outfrom));
                        } else {
                            inpfrom = Optional.empty();
                        }
                        final PipedInputStream inpto = new PipedInputStream();
                        final PipedOutputStream outto = new PipedOutputStream(inpto);
                        final AtomicReference<R> ref = new AtomicReference<>();
                        return CompletableFuture
                            .allOf(
                                CompletableFuture.runAsync(
                                    () -> ref.set(
                                        action.apply(inpfrom, outto)
                                    )
                                ).handle(
                                    inpfrom.map(
                                        stream -> new FutureHandler<>(stream, outto)
                                    ).orElseGet(() -> new FutureHandler<>(outto))
                                ),
                                CompletableFuture.runAsync(
                                    () -> this.asto.save(
                                        this.write,
                                        new Content.From(
                                            new ReactiveInputStream(inpto)
                                                .read(Buffers.Standard.K8)
                                        )
                                    ).join()
                                )
                            ).handle(new FutureHandler<>(inpto))
                            .thenApply(nothing -> ref.get());
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                }
            );
    }

    /**
     * Future's handler to close streams.
     *
     * @param <T> Result type.
     * @since 1.12.0
     */
    private static class FutureHandler<T> implements BiFunction<T, Throwable, T> {
        /**
         * Streams to close.
         */
        private final Closeable[] streams;

        /**
         * Ctor.
         *
         * @param streams Streams to close.
         */
        FutureHandler(final Closeable... streams) {
            this.streams = Arrays.copyOf(streams, streams.length);
        }

        @Override
        public T apply(final T res, final Throwable err) {
            try {
                for (final Closeable stream : this.streams) {
                    stream.close();
                }
            } catch (final IOException ioe) {
                throw new ArtipieIOException(ioe);
            }
            if (err != null) {
                throw new ArtipieIOException(err);
            }
            return res;
        }
    }
}

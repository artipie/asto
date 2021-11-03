/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.stream.ReactiveInputStream;
import org.cqfn.rio.stream.ReactiveOutputStream;

/**
 * Processes storage value content as optional input stream and
 * saves the result back as output stream.
 * @param <R> Result type
 * @since 1.5
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
     * @param asto Abstract storage
     * @param key Item key
     */
    public StorageValuePipeline(final Storage asto, final Key key) {
        this(asto, key, key);
    }

    /**
     * Process storage item and save it back.
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
        ).thenAccept(nothing -> { });
    }

    /**
     * Process storage item, save it back and return some result.
     * @param action Action to perform with storage content if exists and write back as
     *  output stream.
     * @return Completion action with the result
     * @throws ArtipieIOException On Error
     */
    public CompletionStage<R> processWithResult(
        final BiFunction<Optional<InputStream>, OutputStream, R> action
    ) {
        return this.asto.exists(this.read).thenCompose(
            exists -> {
                final CompletionStage<Void> future;
                Optional<InputStream> oinput = Optional.empty();
                Optional<PipedOutputStream> oout = Optional.empty();
                final CompletableFuture<Void> tmp;
                final R result;
                try (PipedOutputStream resout = new PipedOutputStream()) {
                    if (exists) {
                        oinput = Optional.of(new PipedInputStream());
                        final PipedOutputStream tmpout =
                            new PipedOutputStream((PipedInputStream) oinput.get());
                        oout = Optional.of(tmpout);
                        tmp = this.asto.value(this.read).thenCompose(
                            input -> new ReactiveOutputStream(tmpout)
                                .write(input, WriteGreed.SYSTEM)
                        );
                    } else {
                        tmp = CompletableFuture.allOf();
                        oinput = Optional.empty();
                    }
                    final PipedInputStream src = new PipedInputStream(resout);
                    future = tmp.thenCompose(
                        nothing -> this.asto.save(
                            this.write,
                            new Content.From(
                                new ReactiveInputStream(src).read(Buffers.Standard.K8)
                            )
                        )
                    );
                    result = action.apply(oinput, resout);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                } finally {
                    oinput.ifPresent(new UncheckedIOConsumer<>(InputStream::close));
                    oout.ifPresent(new UncheckedIOConsumer<>(PipedOutputStream::close));
                }
                return future.thenApply(nothing -> result);
            }
        );
    }
}

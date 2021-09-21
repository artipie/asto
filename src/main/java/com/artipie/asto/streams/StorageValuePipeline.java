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
import org.cqfn.rio.Buffers;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.stream.ReactiveInputStream;
import org.cqfn.rio.stream.ReactiveOutputStream;

/**
 * Processes storage value content as optional input stream and
 * saves the result back as output stream.
 * @since 1.5
 */
public final class StorageValuePipeline {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Storage item key to process.
     */
    private final Key key;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param key Item key
     */
    public StorageValuePipeline(final Storage asto, final Key key) {
        this.asto = asto;
        this.key = key;
    }

    /**
     * Process storage item and save it back.
     * @param action Action to perform with storage content if exists and write back as
     *  output stream.
     * @return Completion action
     * @throws ArtipieIOException On Error
     */
    CompletionStage<Void> process(final BiConsumer<Optional<InputStream>, OutputStream> action) {
        return this.asto.exists(this.key).thenCompose(
            exists -> {
                final CompletionStage<Void> future;
                Optional<InputStream> oinput = Optional.empty();
                Optional<PipedOutputStream> oout = Optional.empty();
                final CompletableFuture<Void> tmp;
                try (PipedOutputStream resout = new PipedOutputStream()) {
                    if (exists) {
                        oinput = Optional.of(new PipedInputStream());
                        final PipedOutputStream tmpout =
                            new PipedOutputStream((PipedInputStream) oinput.get());
                        oout = Optional.of(tmpout);
                        tmp = this.asto.value(this.key).thenCompose(
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
                            this.key,
                            new Content.From(
                                new ReactiveInputStream(src).read(Buffers.Standard.K8)
                            )
                        )
                    );
                    action.accept(oinput, resout);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                } finally {
                    oinput.ifPresent(new UncheckedIOConsumer<>(InputStream::close));
                    oout.ifPresent(new UncheckedIOConsumer<>(PipedOutputStream::close));
                }
                return future;
            }
        );
    }
}

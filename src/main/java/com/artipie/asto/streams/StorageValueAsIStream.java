/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.stream.ReactiveOutputStream;

/**
 * Process storage value as input stream.
 * This class allows to treat storage item as input stream and
 * perform some action with this stream (read/uncompress/parse etc).
 * @param <T> Result type
 * @since 1.4
 */
public final class StorageValueAsIStream<T> {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Storage item key.
     */
    private final Key key;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param key Storage item key
     */
    public StorageValueAsIStream(final Storage asto, final Key key) {
        this.asto = asto;
        this.key = key;
    }

    /**
     * Process storage item as input stream by performing provided action on it.
     * @param action Action to perform
     * @return Completion action with the result
     */
    public CompletionStage<T> process(final Function<InputStream, T> action) {
        return this.asto.value(this.key).thenCompose(
            value -> {
                try (
                    PipedInputStream in = new PipedInputStream();
                    PipedOutputStream out = new PipedOutputStream(in)
                ) {
                    final CompletionStage<Void> ros =
                        new ReactiveOutputStream(out).write(value, WriteGreed.SYSTEM);
                    final T result = action.apply(in);
                    return ros.thenApply(nothing -> result);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        );
    }
}

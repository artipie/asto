/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#save(Key, Content)} and {@link Storage#value(Key)}.
 *
 * @checkstyle IllegalCatchCheck (500 lines)
 * @since 0.14
 */
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.TooManyMethods"})
@ExtendWith(StorageExtension.class)
public final class StorageSaveAndLoadTest {

    @TestTemplate
    @Timeout(1)
    void shouldSave(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final byte[] data = "0".getBytes();
        final Key key = new Key.From("shouldSave");
        blocking.save(key, data);
        MatcherAssert.assertThat(
            blocking.value(key),
            Matchers.equalTo(data)
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldSaveFromMultipleBuffers(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldSaveFromMultipleBuffers");
        storage.save(
            key,
            new Content.OneTime(
                new Content.From(
                    Flowable.fromArray(
                        ByteBuffer.wrap("12".getBytes()),
                        ByteBuffer.wrap("34".getBytes()),
                        ByteBuffer.wrap("5".getBytes())
                    )
                )
            )
        ).get();
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            Matchers.equalTo("12345".getBytes())
        );
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @TestTemplate
    @Timeout(1)
    void shouldNotOverwriteWithPartial(final Storage storage) {
        final Key key = new Key.From("saveIsAtomic");
        final String initial = "initial";
        storage.save(
            key,
            new Content.OneTime(
                new Content.From(Flowable.fromArray(ByteBuffer.wrap(initial.getBytes())))
            )
        ).join();
        try {
            storage.save(
                key,
                new Content.OneTime(
                    new Content.From(
                        Flowable.concat(
                            Flowable.just(ByteBuffer.wrap(new byte[]{1})),
                            Flowable.error(new IllegalStateException())
                        )
                    )
                )
            ).join();
        } catch (final Exception exc) {
        }
        MatcherAssert.assertThat(
            "save should be atomic",
            new String(new BlockingStorage(storage).value(key)),
            Matchers.equalTo(initial)
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldSaveEmpty(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldSaveEmpty");
        storage.save(key, new Content.OneTime(new Content.From(Flowable.empty()))).get();
        MatcherAssert.assertThat(
            "Saved content should be empty",
            new BlockingStorage(storage).value(key),
            Matchers.equalTo(new byte[0])
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldSaveWhenValueAlreadyExists(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final byte[] original = "1".getBytes();
        final byte[] updated = "2".getBytes();
        final Key key = new Key.From("shouldSaveWhenValueAlreadyExists");
        blocking.save(key, original);
        blocking.save(key, updated);
        MatcherAssert.assertThat(
            blocking.value(key),
            Matchers.equalTo(updated)
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldFailToSaveErrorContent(final Storage storage) throws Exception {
        Assertions.assertThrows(
            Exception.class,
            () -> storage.save(
                new Key.From("shouldFailToSaveErrorContent"),
                new Content.OneTime(new Content.From(Flowable.error(new IllegalStateException())))
            ).join()
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldFailOnSecondConsumeAttempt(final Storage storage) {
        final Key.From key = new Key.From("key");
        storage.save(
            key,
            new Content.OneTime(new Content.From("val".getBytes()))
        ).join();
        final Content value = storage.value(key).join();
        Flowable.fromPublisher(value).toList().blockingGet();
        Assertions.assertThrows(
            ArtipieIOException.class,
            () -> Flowable.fromPublisher(value).toList().blockingGet()
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldFailToLoadAbsentValue(final Storage storage) {
        final CompletableFuture<Content> value = storage.value(
            new Key.From("shouldFailToLoadAbsentValue")
        );
        final Exception exception = Assertions.assertThrows(
            CompletionException.class,
            value::join
        );
        MatcherAssert.assertThat(
            String.format("storage '%s' should fail", storage.getClass().getName()),
            exception.getCause(),
            new IsInstanceOf(ValueNotFoundException.class)
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldNotSavePartial(final Storage storage) {
        final Key key = new Key.From("shouldNotSavePartial");
        storage.save(
            key,
            new Content.From(
                Flowable.concat(
                    Flowable.just(ByteBuffer.wrap(new byte[] {1})),
                    Flowable.error(new IllegalStateException())
                )
            )
        ).exceptionally(ignored -> null).join();
        MatcherAssert.assertThat(
            storage.exists(key).join(),
            Matchers.equalTo(false)
        );
    }

    @TestTemplate
    void shouldReturnContentWithSpecifiedSize(final Storage storage) throws Exception {
        final byte[] content = "1234".getBytes();
        final Key key = new Key.From("shouldReturnContentWithSpecifiedSize");
        storage.save(
            key,
            new Content.OneTime(new Content.From(content))
        ).get();
        MatcherAssert.assertThat(
            storage.value(key).get().size().get(),
            new IsEqual<>((long) content.length)
        );
    }

    @TestTemplate
    void saveDoesNotSupportRootKey(final Storage storage) throws Exception {
        Assertions.assertThrows(
            ExecutionException.class, () -> storage.save(Key.ROOT, Content.EMPTY).get(),
            String.format(
                "`%s` storage didn't fail on root saving",
                storage.getClass().getSimpleName()
            )
        );
    }

    @TestTemplate
    void loadDoesnNotSupportRootKey(final Storage storage) throws Exception {
        Assertions.assertThrows(
            ExecutionException.class, () -> storage.value(Key.ROOT).get(),
            String.format(
                "`%s` storage didn't fail on root loading",
                storage.getClass().getSimpleName()
            )
        );
    }
}

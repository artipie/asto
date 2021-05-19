/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import hu.akarnokd.rxjava2.operators.FlowableTransformers;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Multipart upload of S3 object.
 *
 * @since 0.15
 */
final class MultipartUpload {

    /**
     * Minimum part size. See https://docs.aws.amazon.com/AmazonS3/latest/dev/qfacts.html
     */
    private static final long MIN_PART_SIZE = 5 * 1024 * 1024;

    /**
     * Bucket.
     */
    private final Bucket bucket;

    /**
     * S3 object key.
     */
    private final Key key;

    /**
     * ID of this upload.
     */
    private final String id;

    /**
     * Ctor.
     *
     * @param bucket Bucket.
     * @param key S3 object key.
     * @param id ID of this upload.
     */
    MultipartUpload(final Bucket bucket, final Key key, final String id) {
        this.bucket = bucket;
        this.key = key;
        this.id = id;
    }

    /**
     * Uploads all content by parts.
     *
     * @param content Object content to be uploaded in parts.
     * @return Completion stage which is completed when responses received from S3 for all parts.
     */
    public CompletionStage<Void> upload(final Content content) {
        final AtomicInteger part = new AtomicInteger();
        return Flowable.fromPublisher(content).compose(
            FlowableTransformers.bufferWhile(
                new Predicate<ByteBuffer>() {
                    private long sum;

                    @Override
                    public boolean test(final ByteBuffer buffer) {
                        final int length = buffer.remaining();
                        final boolean keep;
                        if (this.sum + length > MultipartUpload.MIN_PART_SIZE) {
                            this.sum = length;
                            keep = false;
                        } else {
                            this.sum += length;
                            keep = true;
                        }
                        return keep;
                    }
                }
            )
        ).map(
            chunk -> this.uploadPart(
                part.incrementAndGet(),
                Flowable.fromIterable(chunk)
            ).<Void>thenApply(ignored -> null)
        ).reduce(
            CompletableFuture.allOf(),
            (acc, stage) -> acc.thenCompose(o -> stage)
        ).to(SingleInterop.get()).toCompletableFuture().thenCompose(Function.identity());
    }

    /**
     * Completes the upload.
     *
     * @return Completion stage which is completed when success response received from S3.
     */
    public CompletionStage<Void> complete() {
        return this.bucket.completeMultipartUpload(
            CompleteMultipartUploadRequest.builder()
                .key(this.key.string())
                .uploadId(this.id)
                .build()
        ).thenApply(ignored -> null);
    }

    /**
     * Aborts the upload.
     *
     * @return Completion stage which is completed when success response received from S3.
     */
    public CompletionStage<Void> abort() {
        return this.bucket.abortMultipartUpload(
            AbortMultipartUploadRequest.builder()
                .key(this.key.string())
                .uploadId(this.id)
                .build()
        ).thenApply(ignored -> null);
    }

    /**
     * Uploads part.
     *
     * @param part Part number.
     * @param content Part content to be uploaded.
     * @return Completion stage which is completed when success response received from S3.
     */
    private CompletionStage<UploadPartResponse> uploadPart(
        final int part,
        final Publisher<ByteBuffer> content) {
        return Observable.fromPublisher(content)
            .reduce(0L, (total, buf) -> total + buf.remaining())
            .to(SingleInterop.get())
            .toCompletableFuture()
            .thenCompose(
                length -> this.bucket.uploadPart(
                    UploadPartRequest.builder()
                        .key(this.key.string())
                        .uploadId(this.id)
                        .partNumber(part)
                        .contentLength(length)
                        .build(),
                    AsyncRequestBody.fromPublisher(content)
                )
            );
    }
}

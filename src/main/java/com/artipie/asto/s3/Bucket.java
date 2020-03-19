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
package com.artipie.asto.s3;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * S3 client targeted at specific bucket.
 *
 * @since 0.15
 */
final class Bucket {

    /**
     * S3 client.
     */
    private final S3AsyncClient client;

    /**
     * Bucket name.
     */
    private final String name;

    /**
     * Ctor.
     *
     * @param client S3 client.
     * @param name Bucket name.
     */
    Bucket(final S3AsyncClient client, final String name) {
        this.client = client;
        this.name = name;
    }

    /**
     * Handles {@link UploadPartResponse}.
     * See {@link S3AsyncClient#uploadPart(UploadPartRequest, AsyncRequestBody)}
     *
     * @param request Request to bucket.
     * @param body Part body to upload.
     * @return Response to request.
     */
    public CompletableFuture<UploadPartResponse> uploadPart(
        final UploadPartRequest request,
        final AsyncRequestBody body) {
        return this.client.uploadPart(request.copy(original -> original.bucket(this.name)), body);
    }

    /**
     * Handles {@link CompleteMultipartUploadRequest}.
     * See {@link S3AsyncClient#completeMultipartUpload(CompleteMultipartUploadRequest)}
     *
     * @param request Request to bucket.
     * @return Response to request.
     */
    public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        final CompleteMultipartUploadRequest request) {
        return this.client.completeMultipartUpload(
            request.copy(original -> original.bucket(this.name))
        );
    }

    /**
     * Handles {@link AbortMultipartUploadRequest}.
     * See {@link S3AsyncClient#abortMultipartUpload(AbortMultipartUploadRequest)}
     *
     * @param request Request to bucket.
     * @return Response to request.
     */
    public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUpload(
        final AbortMultipartUploadRequest request) {
        return this.client.abortMultipartUpload(
            request.copy(original -> original.bucket(this.name))
        );
    }
}

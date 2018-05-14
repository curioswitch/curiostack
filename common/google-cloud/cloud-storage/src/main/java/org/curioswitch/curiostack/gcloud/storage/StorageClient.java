/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.curioswitch.curiostack.gcloud.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.UrlEscapers;
import com.google.common.util.concurrent.ListenableFuture;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.CommonPools;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.unsafe.ByteBufHttpData;
import com.spotify.futures.CompletableFuturesExtra;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.curioswitch.curiostack.gcloud.storage.StorageModule.ForStorage;
import org.immutables.value.Value.Immutable;

/**
 * A client for Google Cloud Storage for efficient file uploads using armeria. Heap allocations are
 * kept to a minimum, making it appropriate for use with large buffers. Cloud Storage API coverage
 * is kept at a best-effort basis, only common usage for file upload scenarios are targeted.
 */
@Singleton
public class StorageClient {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  private final HttpClient httpClient;

  private final String uploadUrl;
  private final String readUrlPrefix;

  @Inject
  public StorageClient(@ForStorage HttpClient httpClient, StorageConfig config) {
    this.httpClient = httpClient;

    uploadUrl = "/upload/storage/v1/b/" + config.getBucket() + "/o?uploadType=resumable";
    readUrlPrefix = "/storage/v1/b/" + config.getBucket() + "/o/";
  }

  /** Create a new file for uploading data to cloud storage. */
  public ListenableFuture<FileWriter> createFile(
      String filename, Map<String, String> metadata, RequestContext ctx) {
    return CompletableFuturesExtra.toListenableFuture(
        createFile(filename, metadata, ctx.eventLoop(), ctx.alloc()));
  }

  public CompletableFuture<FileWriter> createFile(String filename, Map<String, String> metadata) {
    return createFile(
        filename, metadata, CommonPools.workerGroup().next(), PooledByteBufAllocator.DEFAULT);
  }

  /** Create a new file for uploading data to cloud storage. */
  public CompletableFuture<FileWriter> createFile(
      String filename, Map<String, String> metadata, EventLoop eventLoop, ByteBufAllocator alloc) {
    FileRequest request = ImmutableFileRequest.builder().name(filename).metadata(metadata).build();
    ByteBuf buf = alloc.buffer();
    try (ByteBufOutputStream os = new ByteBufOutputStream(buf)) {
      OBJECT_MAPPER.writeValue((DataOutput) os, request);
    } catch (IOException e) {
      buf.release();
      throw new UncheckedIOException("Could not serialize resource JSON to buffer.", e);
    }

    HttpData data = new ByteBufHttpData(buf, true);

    HttpHeaders headers =
        HttpHeaders.of(HttpMethod.POST, uploadUrl).contentType(MediaType.JSON_UTF_8);
    HttpResponse res = httpClient.execute(headers, data);
    return res.aggregate(eventLoop)
        .handle(
            (msg, t) -> {
              if (t != null) {
                throw new RuntimeException("Unexpected error creating new file.", t);
              }

              HttpHeaders responseHeaders = msg.headers();
              if (!responseHeaders.status().equals(HttpStatus.OK)) {
                throw new RuntimeException(
                    "Non-successful response when creating new file: "
                        + responseHeaders
                        + "\n"
                        + msg.content().toStringUtf8());
              }

              String location = responseHeaders.get(HttpHeaderNames.LOCATION);
              String pathAndQuery = location.substring("https://www.googleapis.com".length());
              return new FileWriter(pathAndQuery, alloc, eventLoop, httpClient);
            });
  }

  /**
   * Reads the contents of a file from cloud storage. Ownership of the returned {@link ByteBuf} is
   * transferred to the caller, which must release it. The future will complete with {@code null} if
   * the file is not found.
   */
  public CompletableFuture<ByteBuf> readFile(String filename) {
    return readFile(filename, CommonPools.workerGroup().next(), PooledByteBufAllocator.DEFAULT);
  }

  /**
   * Reads the contents of a file from cloud storage. Ownership of the returned {@link ByteBuf} is
   * transferred to the caller, which must release it. The future will complete with {@code null} if
   * the file is not found.
   */
  public CompletableFuture<ByteBuf> readFile(
      String filename, EventLoop eventLoop, ByteBufAllocator alloc) {
    String url =
        readUrlPrefix + UrlEscapers.urlPathSegmentEscaper().escape(filename) + "?alt=media";

    return httpClient
        .get(url)
        .aggregateWithPooledObjects(eventLoop, alloc)
        .thenApply(
            msg -> {
              if (msg.status().equals(HttpStatus.NOT_FOUND)) {
                ReferenceCountUtil.safeRelease(msg.content());
                return null;
              }
              HttpData data = msg.content();
              if (data instanceof ByteBufHolder) {
                return ((ByteBufHolder) msg.content()).content();
              } else {
                ByteBuf buf = alloc.buffer(data.length());
                buf.writeBytes(data.array(), data.offset(), data.length());
                return buf;
              }
            });
  }

  @Immutable
  @JsonSerialize(as = ImmutableFileRequest.class)
  @JsonDeserialize(as = ImmutableFileRequest.class)
  interface FileRequest {
    String getName();

    Map<String, String> getMetadata();
  }
}

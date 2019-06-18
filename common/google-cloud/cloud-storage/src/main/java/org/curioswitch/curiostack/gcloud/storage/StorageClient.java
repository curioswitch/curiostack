/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.util.concurrent.ListenableFuture;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.CommonPools;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.curioswitch.curiostack.gcloud.storage.StorageModule.ForStorage;
import org.immutables.value.Value.Derived;
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
  private final String objectUrlPrefix;

  @Inject
  public StorageClient(@ForStorage HttpClient httpClient, StorageConfig config) {
    this.httpClient = httpClient;

    uploadUrl = "/upload/storage/v1/b/" + config.getBucket() + "/o?uploadType=resumable";
    objectUrlPrefix = "/storage/v1/b/" + config.getBucket() + "/o/";
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

  public CompletableFuture<FileWriter> createFile(
      String filename, Map<String, String> metadata, EventLoop eventLoop, ByteBufAllocator alloc) {
    FileRequest request = new FileRequest.Builder().name(filename).metadata(metadata).build();
    return createFile(request, eventLoop, alloc);
  }

  public CompletableFuture<FileWriter> createFile(FileRequest request) {
    return createFile(request, CommonPools.workerGroup().next(), PooledByteBufAllocator.DEFAULT);
  }

  /** Create a new file for uploading data to cloud storage. */
  public CompletableFuture<FileWriter> createFile(
      FileRequest request, EventLoop eventLoop, ByteBufAllocator alloc) {
    HttpData data = serializeRequest(request, alloc);

    RequestHeaders headers =
        RequestHeaders.builder(HttpMethod.POST, uploadUrl)
            .contentType(MediaType.JSON_UTF_8)
            .build();
    HttpResponse res = httpClient.execute(headers, data);
    return res.aggregate(eventLoop)
        .handle(
            (msg, t) -> {
              if (t != null) {
                throw new RuntimeException("Unexpected error creating new file.", t);
              }

              ResponseHeaders responseHeaders = msg.headers();
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
    String url = objectUrlPrefix + urlPathSegmentEscaper().escape(filename) + "?alt=media";

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

  public CompletableFuture<Void> updateFileMetadata(String filename, Map<String, String> metadata) {
    return updateFileMetadata(
        filename, metadata, CommonPools.workerGroup().next(), PooledByteBufAllocator.DEFAULT);
  }

  public CompletableFuture<Void> updateFileMetadata(
      String filename, Map<String, String> metadata, EventLoop eventLoop, ByteBufAllocator alloc) {
    FileRequest request = new FileRequest.Builder().name(filename).metadata(metadata).build();
    return updateFileMetadata(request, eventLoop, alloc);
  }

  public CompletableFuture<Void> updateFileMetadata(
      FileRequest request, EventLoop eventLoop, ByteBufAllocator alloc) {
    String url = objectUrlPrefix + urlPathSegmentEscaper().escape(request.getName());
    return sendMutationRequest(HttpMethod.PUT, request, url, eventLoop, alloc);
  }

  public CompletableFuture<Void> compose(ComposeRequest request) {
    return compose(request, CommonPools.workerGroup().next(), PooledByteBufAllocator.DEFAULT);
  }

  public CompletableFuture<Void> compose(
      ComposeRequest request, EventLoop eventLoop, ByteBufAllocator alloc) {
    String url =
        objectUrlPrefix
            + urlPathSegmentEscaper().escape(request.getDestination().getName())
            + "/compose";
    return sendMutationRequest(HttpMethod.POST, request, url, eventLoop, alloc);
  }

  public CompletableFuture<Void> delete(String filename) {
    String url = objectUrlPrefix + urlPathSegmentEscaper().escape(filename);
    RequestHeaders headers =
        RequestHeaders.builder(HttpMethod.DELETE, url).contentType(MediaType.JSON_UTF_8).build();
    return httpClient
        .execute(headers)
        .aggregate()
        .handle(
            (msg, t) -> {
              if (t != null) {
                throw new RuntimeException("Unexpected error composing file.", t);
              }
              if (msg.status().equals(HttpStatus.OK)) {
                return null;
              } else {
                throw new IllegalStateException(
                    "Could not delete file: " + msg.content().toStringUtf8());
              }
            });
  }

  private CompletableFuture<Void> sendMutationRequest(
      HttpMethod method, Object request, String url, EventLoop eventLoop, ByteBufAllocator alloc) {
    HttpData data = serializeRequest(request, alloc);

    RequestHeaders headers =
        RequestHeaders.builder(HttpMethod.POST, url).contentType(MediaType.JSON_UTF_8).build();
    HttpResponse res = httpClient.execute(headers, data);
    return res.aggregateWithPooledObjects(eventLoop, alloc)
        .handle(
            (msg, t) -> {
              if (t != null) {
                throw new RuntimeException("Unexpected error composing file.", t);
              }
              try {
                if (msg.status().equals(HttpStatus.OK)) {
                  return null;
                } else {
                  throw new IllegalStateException(
                      "Could not compose file: " + msg.content().toStringUtf8());
                }
              } finally {
                ReferenceCountUtil.safeRelease(msg.content());
              }
            });
  }

  private static HttpData serializeRequest(Object request, ByteBufAllocator alloc) {
    ByteBuf buf = alloc.buffer();
    try (ByteBufOutputStream os = new ByteBufOutputStream(buf)) {
      OBJECT_MAPPER.writeValue((DataOutput) os, request);
    } catch (IOException e) {
      buf.release();
      throw new UncheckedIOException("Could not serialize resource JSON to buffer.", e);
    }

    return new ByteBufHttpData(buf, true);
  }

  @Immutable
  @JsonSerialize(as = ImmutableFileRequest.class)
  @JsonDeserialize(as = ImmutableFileRequest.class)
  @JsonInclude(Include.NON_EMPTY)
  public interface FileRequest {

    class Builder extends ImmutableFileRequest.Builder {}

    String getName();

    default String getCacheControl() {
      return "";
    }

    default String getContentDisposition() {
      return "";
    }

    default String getContentEncoding() {
      return "";
    }

    default String getContentLanguage() {
      return "";
    }

    default String getContentType() {
      return "";
    }

    Map<String, String> getMetadata();
  }

  @Immutable
  @JsonSerialize(as = ImmutableComposeRequest.class)
  @JsonDeserialize(as = ImmutableComposeRequest.class)
  public interface ComposeRequest {
    class Builder extends ImmutableComposeRequest.Builder {}

    @Derived
    default String getKind() {
      return "storage#composeRequest";
    }

    @Immutable
    @JsonSerialize(as = ImmutableSourceObject.class)
    @JsonDeserialize(as = ImmutableSourceObject.class)
    interface SourceObject {
      class Builder extends ImmutableSourceObject.Builder {}

      String getName();
    }

    List<SourceObject> getSourceObjects();

    FileRequest getDestination();
  }
}

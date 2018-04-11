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

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpStatusClass;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.unsafe.ByteBufHttpData;
import com.spotify.futures.CompletableFuturesExtra;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.curioswitch.curiostack.gcloud.core.auth.RetryingAuthenticatedGoogleApis;

/**
 * A Cloud Storage file writer. Data should be written using {@link #write(ByteBuffer)}, with the
 * final piece of data being passed to {@link #writeAndClose(ByteBuffer)}. It is fine to call only
 * {@link #writeAndClose(ByteBuffer)} when uploading a single chunk of data.
 *
 * <p>When executing a long-executing file upload over multiple chunks, it is recommended to call
 * {@link #save()} and persist the returned {@link ByteString} into some semi-persistent store
 * (e.g., redis) to allow resuming if the upload gets interrupted.
 */
public class FileWriter {

  /**
   * A Cloud Storage file upload resumer. Should be used to restore a {@link FileWriter} if the
   * upload is cancelled in the middle.
   */
  public static class Resumer {
    private final HttpClient httpClient;

    @Inject
    Resumer(@RetryingAuthenticatedGoogleApis HttpClient httpClient) {
      this.httpClient = httpClient;
    }

    /** Resume a {@link FileWriter} based on the serialized state. */
    public ListenableFuture<FileWriter> resume(ByteString serializedState, RequestContext ctx) {
      final FileWriterState state;
      try {
        state = FileWriterState.parseFrom(serializedState);
      } catch (InvalidProtocolBufferException e) {
        throw new UncheckedIOException("Could not deserialize file writer state.", e);
      }
      final ByteBuf unfinishedChunk;
      if (!state.getUnfinished().isEmpty()) {
        unfinishedChunk = ctx.alloc().buffer(state.getUnfinished().size());
        unfinishedChunk.writeBytes(state.getUnfinished().asReadOnlyByteBuffer());
      } else {
        unfinishedChunk = null;
      }

      String url =
          state.getUploadUrl().startsWith("/upload/storage/v1")
              ? state.getUploadUrl()
              : "/upload/storage/v1" + state.getUploadUrl();

      FileWriter writer =
          new FileWriter(url, ctx, httpClient, state.getFilePosition(), unfinishedChunk);
      return immediateFuture(writer);
    }
  }

  // All chunks must be a multiple of 256KB except the last one.
  private static final int CHUNK_ALIGNMENT = 256 * 1024;

  private final String uploadUrl;
  private final HttpClient httpClient;
  private final ByteBufAllocator alloc;
  private final EventLoop eventLoop;

  private long filePosition;
  @Nullable private ByteBuf unfinishedChunk;

  FileWriter(String uploadUrl, ByteBufAllocator alloc, EventLoop eventLoop, HttpClient httpClient) {
    this.uploadUrl = uploadUrl;
    this.httpClient = httpClient;
    this.alloc = alloc;
    this.eventLoop = eventLoop;
  }

  FileWriter(
      String uploadUrl,
      RequestContext ctx,
      HttpClient httpClient,
      long filePosition,
      @Nullable ByteBuf unfinishedChunk) {
    this.uploadUrl = uploadUrl;
    this.httpClient = httpClient;
    this.filePosition = filePosition;
    this.unfinishedChunk = unfinishedChunk;
    alloc = ctx.alloc();
    eventLoop = ctx.contextAwareEventLoop();
  }

  /**
   * Writes the {@link ByteBuffer} to the file. When possible, {@code data} is not copied before
   * writing, so the caller must ensure it is not changed and survives until the future is
   * completed. This may mean copying to a new buffer or increasing a reference count as
   * appropriate.
   *
   * <p>When possible, {@code data} should have a size that is a multiple of 256KB. This will ensure
   * minimal copies and optimal performance.
   */
  public ListenableFuture<Void> write(ByteBuffer data) {
    ByteBuf nextBuf = Unpooled.wrappedBuffer(data);

    final ByteBuf buf;
    if (unfinishedChunk == null) {
      buf = nextBuf;
    } else {
      buf =
          alloc.compositeBuffer(2).addComponent(true, unfinishedChunk).addComponent(true, nextBuf);
      unfinishedChunk = null;
    }

    int alignedWritableBytes = alignedSize(buf.readableBytes());
    if (alignedWritableBytes == buf.readableBytes()) {
      return CompletableFuturesExtra.toListenableFuture(uploadChunk(buf, false));
    }

    if (alignedWritableBytes == 0) {
      // Not enough data for a chunk, so copy it for next time.
      copyUnfinishedBuffer(buf);
      return immediateFuture(null);
    }

    ByteBuf nextChunk = buf.readSlice(alignedWritableBytes);
    copyUnfinishedBuffer(buf);
    return CompletableFuturesExtra.toListenableFuture(uploadChunk(nextChunk, false));
  }

  /**
   * Writes the {@link ByteBuffer} to the file and closes it. No further writes will be possible.
   * When possible, {@code data} is not copied before writing, so the caller must ensure it is not
   * changed and survives until the future is completed. This may mean copying to a new buffer or
   * increasing a reference count as appropriate.
   */
  public ListenableFuture<Void> writeAndClose(ByteBuffer data) {
    return CompletableFuturesExtra.toListenableFuture(writeAndClose(Unpooled.wrappedBuffer(data)));
  }

  public CompletableFuture<Void> writeAndClose(ByteBuf data) {
    if (unfinishedChunk == null) {
      return uploadChunk(data, true);
    } else {
      ByteBuf nextChunk = unfinishedChunk;
      unfinishedChunk = null;
      nextChunk.writeBytes(data);
      return uploadChunk(nextChunk, true);
    }
  }

  /**
   * Save the state of the {@link FileWriter} to allow resuming using {@link Resumer} if needed. The
   * returned buffer will be up to 256KB large depending on the state of the file upload. Writing in
   * chunks that are a multiple of 256KB minimizes the chance of this, but does not guarantee
   * eliminating it so all callers should be prepared for such a size.
   */
  public ByteString save() {
    FileWriterState.Builder builder =
        FileWriterState.newBuilder().setUploadUrl(uploadUrl).setFilePosition(filePosition);
    if (unfinishedChunk != null) {
      builder.setUnfinished(ByteString.copyFrom(unfinishedChunk.nioBuffer().slice()));
    }
    return builder.build().toByteString();
  }

  /**
   * Releases any buffers owned by this {@link FileWriter}. This should be called in error-cases
   * where the file upload will be resumed with a different {@link FileWriter}.
   */
  public void release() {
    if (unfinishedChunk != null) {
      unfinishedChunk.release();
    }
  }

  private CompletableFuture<Void> uploadChunk(ByteBuf chunk, boolean endOfFile) {
    return doUploadChunk(chunk, endOfFile);
  }

  private CompletableFuture<Void> doUploadChunk(ByteBuf chunk, boolean endOfFile) {

    int length = chunk.readableBytes();
    long limit = filePosition + length;

    StringBuilder range = new StringBuilder("bytes ");
    if (length == 0) {
      range.append('*');
    } else {
      range.append(filePosition).append('-').append(limit - 1);
    }
    range.append('/');
    if (endOfFile) {
      range.append(limit);
    } else {
      range.append('*');
    }

    HttpHeaders headers =
        HttpHeaders.of(HttpMethod.PUT, uploadUrl)
            .set(HttpHeaderNames.CONTENT_RANGE, range.toString());

    HttpData data = new ByteBufHttpData(chunk, true);
    chunk.retain();

    return httpClient
        .execute(headers, data)
        .aggregate(eventLoop)
        .thenComposeAsync(
            msg -> {
              HttpHeaders responseHeaders = msg.headers();
              if (!responseHeaders.status().codeClass().equals(HttpStatusClass.SUCCESS)
                  && responseHeaders.status().code() != 308) {
                chunk.release();
                throw new RuntimeException(
                    "Unsuccessful response uploading chunk: endOfFile: "
                        + endOfFile
                        + " Request headers: "
                        + headers
                        + "\n"
                        + " Response headers: "
                        + responseHeaders
                        + "\n"
                        + msg.content().toStringUtf8());
              }

              String responseRange = responseHeaders.get(HttpHeaderNames.RANGE);
              if (responseRange == null) {
                chunk.release();
                return completedFuture(null);
              }

              long responseLimit = rangeHeaderLimit(responseHeaders.get(HttpHeaderNames.RANGE));
              filePosition = responseLimit + 1;
              int notUploaded = (int) (limit - 1 - responseLimit);
              if (notUploaded > 0) {
                chunk.readerIndex(chunk.writerIndex() - notUploaded);

                if (endOfFile) {
                  return doUploadChunk(chunk, true);
                }

                if (unfinishedChunk == null) {
                  copyUnfinishedBuffer(chunk);
                } else {
                  ByteBuf newUnfinished =
                      alloc.buffer(chunk.readableBytes() + unfinishedChunk.readableBytes());
                  newUnfinished.writeBytes(chunk).writeBytes(unfinishedChunk);
                  unfinishedChunk.release();
                  unfinishedChunk = newUnfinished;
                }
              }
              chunk.release();
              return completedFuture(null);
            },
            eventLoop);
  }

  private void copyUnfinishedBuffer(ByteBuf buf) {
    unfinishedChunk = alloc.buffer(nextAlignedSize(buf.readableBytes()));
    unfinishedChunk.writeBytes(buf);
  }

  private static long rangeHeaderLimit(String range) {
    return Long.parseLong(range.substring(range.indexOf('-') + 1));
  }

  private static int alignedSize(int num) {
    return (num / CHUNK_ALIGNMENT) * CHUNK_ALIGNMENT;
  }

  private static int nextAlignedSize(int num) {
    if (num % CHUNK_ALIGNMENT == 0) {
      return num;
    }

    return ((num / CHUNK_ALIGNMENT) + 1) * CHUNK_ALIGNMENT;
  }
}

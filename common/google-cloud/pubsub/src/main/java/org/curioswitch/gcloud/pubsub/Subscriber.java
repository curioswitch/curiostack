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

package org.curioswitch.gcloud.pubsub;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.StreamingPullRequest;
import com.google.pubsub.v1.StreamingPullResponse;
import com.google.pubsub.v1.SubscriberGrpc.SubscriberStub;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.client.grpc.GrpcClientOptions;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.unsafe.grpc.GrpcUnsafeBufferUtil;
import io.grpc.stub.StreamObserver;
import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.curioswitch.common.helpers.immutables.CurioStyle;
import org.curioswitch.gcloud.pubsub.Subscriber.Factory;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@AutoFactory(implementing = Factory.class)
public class Subscriber implements Closeable, StreamObserver<StreamingPullResponse> {

  public interface Factory {
    Subscriber create(SubscriberOptions options);
  }

  private static final Duration INITIAL_CHANNEL_RECONNECT_BACKOFF = Duration.ofMillis(100);
  private static final Duration MAX_CHANNEL_RECONNECT_BACKOFF = Duration.ofSeconds(10);

  private final SubscriberStub stub;
  private final SubscriberOptions options;

  private Duration streamReconnectBackoff = INITIAL_CHANNEL_RECONNECT_BACKOFF;

  @Nullable private StreamObserver<StreamingPullRequest> requestObserver;
  @Nullable private RequestContext ctx;

  private volatile boolean closed;

  public Subscriber(@Provided SubscriberStub stub, SubscriberOptions options) {
    this.stub =
        options.getUnsafeWrapBuffers()
            ? Clients.newDerivedClient(
                stub, GrpcClientOptions.UNSAFE_WRAP_RESPONSE_BUFFERS.newValue(true))
            : stub;
    this.options = options;
  }

  public void start() {
    open();
  }

  @Override
  public void onNext(StreamingPullResponse value) {
    if (ctx == null) {
      ctx = RequestContext.current();
    }

    streamReconnectBackoff = INITIAL_CHANNEL_RECONNECT_BACKOFF;

    for (ReceivedMessage message : value.getReceivedMessagesList()) {
      options
          .getMessageReceiver()
          .receiveMessage(
              message.getMessage(),
              new AckReplyConsumer() {
                @Override
                public void ack() {
                  release();

                  checkNotNull(requestObserver, "onNext called before start()");
                  requestObserver.onNext(
                      StreamingPullRequest.newBuilder().addAckIds(message.getAckId()).build());
                }

                @Override
                public void nack() {
                  release();

                  checkNotNull(requestObserver, "onNext called before start()");
                  requestObserver.onNext(
                      StreamingPullRequest.newBuilder()
                          .addModifyDeadlineAckIds(message.getAckId())
                          .addModifyDeadlineSeconds(0)
                          .build());
                }

                private void release() {
                  if (options.getUnsafeWrapBuffers()) {
                    GrpcUnsafeBufferUtil.releaseBuffer(message, ctx);
                  }
                }
              });
    }
  }

  @Override
  public void onError(Throwable t) {
    if (closed || !StatusUtil.isRetryable(t)) {
      return;
    }

    Duration backoff = streamReconnectBackoff;

    streamReconnectBackoff = streamReconnectBackoff.multipliedBy(2);
    if (streamReconnectBackoff.compareTo(MAX_CHANNEL_RECONNECT_BACKOFF) > 0) {
      streamReconnectBackoff = MAX_CHANNEL_RECONNECT_BACKOFF;
    }

    // Possible to come straight to here without onNext, so access the current RequestContext
    // regardless.
    RequestContext.current()
        .eventLoop()
        .schedule(this::open, backoff.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void onCompleted() {
    if (closed) {
      return;
    }

    streamReconnectBackoff = INITIAL_CHANNEL_RECONNECT_BACKOFF;

    // Stream was closed by the server, reopen it so we can keep on pulling.
    open();
  }

  @Override
  public void close() {
    checkNotNull(requestObserver, "close called before start.");

    closed = true;
    requestObserver.onCompleted();
  }

  // synchronized just to make sure requestObserver is written out for the callback thread.
  private synchronized void open() {
    // Reset in case this is a reconnect.
    ctx = null;

    requestObserver = stub.streamingPull(this);

    requestObserver.onNext(
        StreamingPullRequest.newBuilder()
            .setSubscription(options.getSubscription())
            .setStreamAckDeadlineSeconds(60)
            .build());
  }

  @Immutable
  @CurioStyle
  @Style(build = "buildOptions")
  interface SubscriberOptions {

    class Builder extends ImmutableSubscriberOptions.Builder {}

    /** Cloud Pub/Sub subscription to bind the subscriber to. */
    String getSubscription();

    /** An implementation of {@link MessageReceiver} used to process the received messages. */
    MessageReceiver getMessageReceiver();

    /**
     * Whether {@link com.google.protobuf.ByteString} should wrap incoming network buffers instead
     * of copying. Can improve performance when dealing with large messages. If {@code true}, a
     * {@link com.google.pubsub.v1.PubsubMessage} must not be accessed after {@link
     * AckReplyConsumer#ack()} or {@link AckReplyConsumer#nack()} has been called.
     */
    default boolean getUnsafeWrapBuffers() {
      return false;
    }
  }
}

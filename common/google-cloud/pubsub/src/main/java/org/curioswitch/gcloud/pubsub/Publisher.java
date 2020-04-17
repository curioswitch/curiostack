/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PublishRequest;
import com.google.pubsub.v1.PublisherGrpc.PublisherFutureStub;
import com.google.pubsub.v1.PubsubMessage;
import org.curioswitch.common.helpers.immutables.CurioStyle;
import org.curioswitch.gcloud.pubsub.Publisher.Factory;
import org.immutables.value.Value.Immutable;

@AutoFactory(implementing = Factory.class)
public class Publisher {

  public interface Factory {
    Publisher create(PublisherOptions options);
  }

  public static PublisherOptions.Builder newOptions(String topic) {
    return new PublisherOptions.Builder().topic(topic);
  }

  public static PublisherOptions.Builder newOptions(ProjectTopicName topic) {
    return new PublisherOptions.Builder().topic(topic.toString());
  }

  private final PublisherFutureStub stub;
  private final Tracer tracer;
  private final PublisherOptions options;
  private final TraceContext.Injector<PubsubMessage.Builder> traceInjector;

  public Publisher(
      @Provided PublisherFutureStub stub, @Provided Tracing tracing, PublisherOptions options) {
    this.stub = checkNotNull(stub, "stub");
    this.options = checkNotNull(options, "options");
    checkNotNull(tracing, "tracing");

    tracer = tracing.tracer();
    traceInjector = tracing.propagation().injector(PubsubMessage.Builder::putAttributes);
  }

  public ListenableFuture<String> publish(PubsubMessage message) {
    Span span = tracer.currentSpan();
    if (span != null) {
      PubsubMessage.Builder messageBuilder = message.toBuilder();
      traceInjector.inject(span.context(), messageBuilder);
      message = messageBuilder.build();
    }

    PublishRequest request =
        PublishRequest.newBuilder().setTopic(options.getTopic()).addMessages(message).build();

    return Futures.transform(
        stub.publish(request),
        (response) -> {
          if (response.getMessageIdsCount() != 1) {
            throw new IllegalStateException(
                String.format(
                    "The publish result count %s does not match "
                        + "the expected 1 result. Please contact Cloud Pub/Sub support "
                        + "if this frequently occurs",
                    response.getMessageIdsCount()));
          }
          return response.getMessageIds(0);
        },
        MoreExecutors.directExecutor());
  }

  @Immutable
  @CurioStyle
  public interface PublisherOptions {
    class Builder extends ImmutablePublisherOptions.Builder {}

    /** Topic which the publisher publishes to. */
    String getTopic();
  }
}

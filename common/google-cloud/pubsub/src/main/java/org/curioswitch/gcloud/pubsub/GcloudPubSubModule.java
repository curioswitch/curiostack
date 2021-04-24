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

package org.curioswitch.gcloud.pubsub;

import com.google.pubsub.v1.PublisherGrpc.PublisherFutureStub;
import com.google.pubsub.v1.SubscriberGrpc.SubscriberFutureStub;
import com.google.pubsub.v1.SubscriberGrpc.SubscriberStub;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import java.time.Duration;
import org.curioswitch.curiostack.gcloud.core.auth.GcloudAuthModule;
import org.curioswitch.curiostack.gcloud.core.grpc.GrpcApiClientBuilder;

@Module(includes = GcloudAuthModule.class)
public abstract class GcloudPubSubModule {

  @Binds
  abstract Subscriber.Factory subscriberFactory(SubscriberFactory factory);

  @Binds
  abstract Publisher.Factory publisherFactory(PublisherFactory factory);

  @Provides
  static PublisherFutureStub publisher(GrpcApiClientBuilder clientBuilder) {
    return clientBuilder.create("https://pubsub.googleapis.com/", PublisherFutureStub.class);
  }

  @Provides
  static SubscriberFutureStub subscriber(GrpcApiClientBuilder clientBuilder) {
    return clientBuilder.create("https://pubsub.googleapis.com/", SubscriberFutureStub.class);
  }

  @Provides
  static SubscriberStub streamingSubscriber(GrpcApiClientBuilder clientBuilder) {
    return clientBuilder
        .newBuilder("https://pubsub.googleapis.com/")
        .maxResponseLength(Integer.MAX_VALUE)
        .responseTimeout(Duration.ZERO)
        .build(SubscriberStub.class);
  }

  private GcloudPubSubModule() {}
}

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

package org.curioswitch.curiostack.gcloud.core.grpc;

import brave.Tracing;
import com.google.common.base.Strings;
import com.linecorp.armeria.client.ClientBuilder;
import com.linecorp.armeria.client.endpoint.EndpointGroupRegistry;
import com.linecorp.armeria.client.endpoint.EndpointSelectionStrategy;
import com.linecorp.armeria.client.endpoint.dns.DnsAddressEndpointGroup;
import com.linecorp.armeria.client.metric.MetricCollectingClient;
import com.linecorp.armeria.client.tracing.HttpTracingClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import javax.inject.Inject;
import org.curioswitch.curiostack.gcloud.core.auth.GoogleCredentialsDecoratingClient;
import org.curioswitch.curiostack.gcloud.core.auth.GoogleCredentialsDecoratingClient.Factory;

public class GrpcApiClientBuilder {

  private final MeterRegistry meterRegistry;
  private final Tracing tracing;
  private final GoogleCredentialsDecoratingClient.Factory credentialsDecorator;

  @Inject
  public GrpcApiClientBuilder(
      MeterRegistry meterRegistry,
      Tracing tracing,
      Factory credentialsDecorator) {
    this.meterRegistry = meterRegistry;
    this.tracing = tracing;
    this.credentialsDecorator = credentialsDecorator;
  }

  public <T> T create(String name, String url, Class<T> clz) {
    URI uri = URI.create(url);
    DnsAddressEndpointGroup endpointGroup =
        DnsAddressEndpointGroup.of(uri.getHost(), uri.getPort());
    endpointGroup.start();
    EndpointGroupRegistry.register(name, endpointGroup, EndpointSelectionStrategy.ROUND_ROBIN);
    return new ClientBuilder(uri.getScheme() + "://group:" + name + Strings.nullToEmpty(uri.getPath()))
        .decorator(HttpRequest.class, HttpResponse.class, credentialsDecorator.newAccessTokenDecorator())
        .decorator(HttpRequest.class, HttpResponse.class, HttpTracingClient.newDecorator(tracing))
        .decorator(HttpRequest.class, HttpResponse.class, MetricCollectingClient.newDecorator(MetricLabels.grpcRequestLabeler()))
        .build(clz);
  }
}

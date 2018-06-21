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
import com.linecorp.armeria.client.ClientBuilder;
import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.SimpleDecoratingClient;
import com.linecorp.armeria.client.logging.LoggingClientBuilder;
import com.linecorp.armeria.client.metric.MetricCollectingClient;
import com.linecorp.armeria.client.tracing.HttpTracingClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import javax.inject.Inject;
import org.curioswitch.curiostack.gcloud.core.auth.GoogleCredentialsDecoratingClient;

public class GrpcApiClientBuilder {

  private final Tracing tracing;
  private final GoogleCredentialsDecoratingClient.Factory credentialsDecorator;

  @Inject
  public GrpcApiClientBuilder(
      Tracing tracing, GoogleCredentialsDecoratingClient.Factory credentialsDecorator) {
    this.tracing = tracing;
    this.credentialsDecorator = credentialsDecorator;
  }

  public <T> T create(String url, Class<T> clz) {
    return new ClientBuilder("gproto+" + url)
        .decorator(HttpRequest.class, HttpResponse.class, client -> new SimpleDecoratingClient<HttpRequest, HttpResponse>(client) {
          @Override
          public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
            req.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/grpc");
            return delegate().execute(ctx, req);
          }
        })
        .decorator(
            HttpRequest.class, HttpResponse.class, credentialsDecorator.newAccessTokenDecorator())
        .decorator(HttpRequest.class, HttpResponse.class, HttpTracingClient.newDecorator(tracing))
        .decorator(
            HttpRequest.class,
            HttpResponse.class,
            MetricCollectingClient.newDecorator(MetricLabels.grpcRequestLabeler()))
        .decorator(HttpRequest.class, HttpResponse.class, new LoggingClientBuilder().newDecorator())
        .build(clz);
  }
}

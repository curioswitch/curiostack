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
package org.curioswitch.curiostack.gcloud.core.auth;

import com.google.auth.oauth2.ComputeEngineCredentials;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import java.net.URI;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

class ComputeEngineAccessTokenProvider extends AbstractAccessTokenProvider {

  private static final AsciiString METADATA_FLAVOR_HEADER = HttpHeaderNames.of("Metadata-Flavor");

  @Inject
  ComputeEngineAccessTokenProvider(WebClient googleApisClient, Clock clock) {
    super(googleApisClient, clock);
  }

  @Override
  ByteBuf refreshRequestContent(Type type) {
    throw new UnsupportedOperationException("Compute Engine access token uses GET request.");
  }

  @Override
  protected CompletableFuture<AggregatedHttpResponse> fetchToken(Type type) {
    URI uri = URI.create(ComputeEngineCredentials.getTokenServerEncodedUrl());

    // In practice, this URL shouldn't change at runtime but it's not infeasible, and since this
    // shouldn't be executed often, just create a client every time.
    WebClient client =
        WebClient.builder("h1c://" + uri.getAuthority() + "/")
            .decorator(LoggingClient.builder().newDecorator())
            .build();
    return client
        .execute(RequestHeaders.of(HttpMethod.GET, uri.getPath(), METADATA_FLAVOR_HEADER, "Google"))
        .aggregate();
  }
}

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

package org.curioswitch.curiostack.gcloud.core.auth;

import com.google.auth.oauth2.ComputeEngineCredentials;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.AggregatedHttpMessage;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpMethod;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

class ComputeEngineAccessTokenProvider extends AbstractAccessTokenProvider {

  private static final AsciiString METADATA_FLAVOR_HEADER = HttpHeaderNames.of("Metadata-Flavor");

  @Inject
  ComputeEngineAccessTokenProvider(HttpClient googleApisClient, Clock clock) {
    super(googleApisClient, clock);
  }

  @Override
  ByteBuf refreshRequestContent(Type type) {
    throw new UnsupportedOperationException("Compute Engine access token uses GET request.");
  }

  @Override
  protected CompletableFuture<AggregatedHttpMessage> fetchToken(
      Type type, HttpClient googleApisClient) {
    return googleApisClient
        .execute(
            HttpHeaders.of(HttpMethod.GET, ComputeEngineCredentials.getTokenServerEncodedUrl())
                .set(METADATA_FLAVOR_HEADER, "Google"))
        .aggregate();
  }
}

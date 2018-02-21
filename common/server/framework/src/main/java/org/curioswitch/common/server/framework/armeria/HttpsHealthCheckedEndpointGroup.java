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

package org.curioswitch.common.server.framework.armeria;

import static java.util.Objects.requireNonNull;

import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.endpoint.EndpointGroup;
import com.linecorp.armeria.client.endpoint.healthcheck.HealthCheckedEndpointGroup;
import com.linecorp.armeria.common.HttpStatus;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Fork of {@link com.linecorp.armeria.client.endpoint.healthcheck.HttpHealthCheckedEndpointGroup}
 * which uses HTTPS.
 */
public class HttpsHealthCheckedEndpointGroup extends HealthCheckedEndpointGroup {

  private final String healthCheckPath;

  /** Creates a new {@link HttpsHealthCheckedEndpointGroup} instance. */
  public static HttpsHealthCheckedEndpointGroup of(EndpointGroup delegate, String healthCheckPath) {
    return of(delegate, healthCheckPath, DEFAULT_HEALTHCHECK_RETRY_INTERVAL);
  }

  /** Creates a new {@link HttpsHealthCheckedEndpointGroup} instance. */
  public static HttpsHealthCheckedEndpointGroup of(
      EndpointGroup delegate, String healthCheckPath, Duration healthCheckRetryInterval) {
    return of(ClientFactory.DEFAULT, delegate, healthCheckPath, healthCheckRetryInterval);
  }

  /** Creates a new {@link HttpsHealthCheckedEndpointGroup} instance. */
  public static HttpsHealthCheckedEndpointGroup of(
      ClientFactory clientFactory,
      EndpointGroup delegate,
      String healthCheckPath,
      Duration healthCheckRetryInterval) {
    return new HttpsHealthCheckedEndpointGroup(
        clientFactory, delegate, healthCheckPath, healthCheckRetryInterval);
  }

  /** Creates a new {@link HttpsHealthCheckedEndpointGroup} instance. */
  private HttpsHealthCheckedEndpointGroup(
      ClientFactory clientFactory,
      EndpointGroup delegate,
      String healthCheckPath,
      Duration healthCheckRetryInterval) {
    super(clientFactory, delegate, healthCheckRetryInterval);
    this.healthCheckPath = requireNonNull(healthCheckPath, "healthCheckPath");
    init();
  }

  @Override
  protected EndpointHealthChecker createEndpointHealthChecker(Endpoint endpoint) {
    return new HttpsHealthCheckedEndpointGroup.HttpEndpointHealthChecker(
        clientFactory(), endpoint, healthCheckPath);
  }

  private static final class HttpEndpointHealthChecker implements EndpointHealthChecker {
    private final HttpClient httpClient;
    private final String healthCheckPath;

    private HttpEndpointHealthChecker(
        ClientFactory clientFactory, Endpoint endpoint, String healthCheckPath) {
      httpClient = HttpClient.of(clientFactory, "https://" + endpoint.authority());
      this.healthCheckPath = healthCheckPath;
    }

    @Override
    public CompletableFuture<Boolean> isHealthy(Endpoint endpoint) {
      return httpClient
          .get(healthCheckPath)
          .aggregate()
          .thenApply(message -> message.status().equals(HttpStatus.OK));
    }
  }
}

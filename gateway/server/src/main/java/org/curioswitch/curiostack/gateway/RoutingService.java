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
package org.curioswitch.curiostack.gateway;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.Flags;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Route;
import com.linecorp.armeria.server.RoutingContext;
import com.linecorp.armeria.server.ServiceRequestContext;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class RoutingService implements HttpService {

  private static final Logger logger = LogManager.getLogger();

  @Nullable private final LoadingCache<RoutingContext, WebClient> pathClients;
  private final boolean cachePaths;

  private volatile Map<Route, WebClient> clients;

  @SuppressWarnings("ConstructorLeaksThis")
  RoutingService(Map<Route, WebClient> clients) {
    this.clients = clients;

    if (Flags.parsedPathCacheSpec() != null) {
      cachePaths = true;
      pathClients = Caffeine.from(Flags.parsedPathCacheSpec()).build(this::find);
    } else {
      cachePaths = false;
      pathClients = null;
    }
  }

  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) {
    RoutingContext mappingContext = ctx.routingContext();
    final WebClient client;
    if (pathClients != null && mappingContext.query() == null) {
      client = pathClients.get(mappingContext);
    } else {
      client = find(mappingContext);
    }
    if (client == null) {
      return HttpResponse.of(HttpStatus.NOT_FOUND);
    }
    // We don't want to pass the external domain name through to the backend server since this
    // causes problems with the TLS handshake between this server and the backend (the external
    // hostname does not match the names we use in our certs for server to server communication).
    req = req.withHeaders(req.headers().toBuilder().authority("").build());
    return client.execute(req);
  }

  @Override
  public boolean shouldCachePath(String path, @Nullable String query, Route pathMapping) {
    return this.cachePaths;
  }

  @Nullable
  private WebClient find(RoutingContext mappingContext) {
    return clients.entrySet().stream()
        .filter(entry -> entry.getKey().apply(mappingContext).isPresent())
        .map(Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  void updateClients(Map<Route, WebClient> clients) {
    logger.info("Updating router targets.");
    this.clients = clients;
    pathClients.invalidateAll();
  }
}

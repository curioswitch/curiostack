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

package org.curioswitch.curiostack.gateway;

import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.PathMapping;
import com.linecorp.armeria.server.PathMappingContext;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.VirtualHost;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.Nullable;

class RoutingService implements HttpService {

  private volatile Map<PathMapping, HttpClient> clients;

  RoutingService(Map<PathMapping, HttpClient> clients) {
    this.clients = clients;
  }

  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) {
    PathOnlyMappingContext mappingContext = new PathOnlyMappingContext(ctx);

    return clients
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().apply(mappingContext).isPresent())
        .map(Entry::getValue)
        .findFirst()
        .map(httpClient -> httpClient.execute(req))
        .orElseGet(() -> HttpResponse.of(HttpStatus.NOT_FOUND));
  }

  void updateClients(Map<PathMapping, HttpClient> clients) {
    this.clients = clients;
  }

  private static class PathOnlyMappingContext implements PathMappingContext {

    private final ServiceRequestContext ctx;

    private PathOnlyMappingContext(ServiceRequestContext ctx) {
      this.ctx = ctx;
    }

    @Override
    public VirtualHost virtualHost() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String hostname() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpMethod method() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String path() {
      return ctx.path();
    }

    @Nullable
    @Override
    public String query() {
      return ctx.query();
    }

    @Nullable
    @Override
    public MediaType consumeType() {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public List<MediaType> produceTypes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> summary() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delayThrowable(Throwable cause) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Throwable> delayedThrowable() {
      throw new UnsupportedOperationException();
    }
  }
}

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
package org.curioswitch.common.server.framework.staticsite;

import com.linecorp.armeria.common.FilteredHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpObject;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingService;
import java.util.function.Function;

/**
 * A {@link SimpleDecoratingService} which set infinite-expiration cache headers. Should only be
 * used for static resources with unique URLs.
 */
public class InfiniteCachingService extends SimpleDecoratingService<HttpRequest, HttpResponse> {

  public static Function<Service<HttpRequest, HttpResponse>, InfiniteCachingService>
      newDecorator() {
    return service -> new InfiniteCachingService(service);
  }

  /** Creates a new instance that decorates the specified {@link Service}. */
  private InfiniteCachingService(Service<HttpRequest, HttpResponse> delegate) {
    super(delegate);
  }

  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
    return new CacheHeaderSettingResponse(delegate().serve(ctx, req));
  }

  private static class CacheHeaderSettingResponse extends FilteredHttpResponse {

    private CacheHeaderSettingResponse(HttpResponse delegate) {
      super(delegate, true);
    }

    @Override
    protected HttpObject filter(HttpObject obj) {
      if (!(obj instanceof HttpHeaders)) {
        return obj;
      }
      HttpHeaders headers = (HttpHeaders) obj;
      return headers
          .add(HttpHeaderNames.CACHE_CONTROL, "public,max-age=31536000,immutable")
          .add(HttpHeaderNames.VARY, "Accept-Encoding");
    }
  }
}

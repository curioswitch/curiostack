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
package org.curioswitch.common.server.framework.security;

import com.google.common.base.Ascii;
import com.google.common.base.Strings;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingService;
import io.netty.util.AsciiString;
import java.util.function.Function;
import javax.inject.Inject;
import org.curioswitch.common.server.framework.config.SecurityConfig;

/**
 * A {@link SimpleDecoratingService} which redirects HTTP requests from a proxy to HTTPS. Meant for
 * use in an externally-facing, production frontend server (restricted servers should use an
 * authenticating proxy like Identity-Aware Proxy, which handles redirects itself).
 *
 * <p>Insecure HTTP requests are redirected to HTTPS while HTTPS responses have best practice secure
 * headers such as Strict-Transport-Security and embed blocking.
 */
public class HttpsOnlyService extends SimpleDecoratingService<HttpRequest, HttpResponse> {

  public static class Factory {
    private final SecurityConfig config;

    @Inject
    public Factory(SecurityConfig config) {
      this.config = config;
    }

    public Function<Service<HttpRequest, HttpResponse>, HttpsOnlyService> newDecorator() {
      return service -> new HttpsOnlyService(service, config);
    }
  }

  private static final AsciiString STRICT_TRANSPORT_SECURITY =
      HttpHeaderNames.of("Strict-Transport-Security");
  private static final AsciiString X_FORWARDED_PROTO = HttpHeaderNames.of("X-Forwarded-Proto");

  private final SecurityConfig config;

  /** Creates a new instance that decorates the specified {@link Service}. */
  private HttpsOnlyService(Service<HttpRequest, HttpResponse> delegate, SecurityConfig config) {
    super(delegate);
    this.config = config;
  }

  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
    String xForwardedProto =
        Ascii.toLowerCase(Strings.nullToEmpty(req.headers().get(X_FORWARDED_PROTO)));
    if (xForwardedProto.isEmpty() || xForwardedProto.equals("https")) {
      ctx.addAdditionalResponseHeader(STRICT_TRANSPORT_SECURITY, "max-age=31536000; preload");
      ctx.addAdditionalResponseHeader(HttpHeaderNames.X_FRAME_OPTIONS, "DENY");
      config
          .getAdditionalResponseHeaders()
          .forEach(
              (key, value) ->
                  ctx.addAdditionalResponseHeader(HttpHeaderNames.of(key), (String) value));
      return delegate().serve(ctx, req);
    }
    StringBuilder redirectUrl =
        new StringBuilder("https://" + req.headers().authority() + ctx.path());
    if (ctx.query() != null) {
      redirectUrl.append('?').append(ctx.query());
    }
    return HttpResponse.of(
        HttpResponse.of(
            ResponseHeaders.of(
                HttpStatus.MOVED_PERMANENTLY, HttpHeaderNames.LOCATION, redirectUrl.toString())));
  }
}

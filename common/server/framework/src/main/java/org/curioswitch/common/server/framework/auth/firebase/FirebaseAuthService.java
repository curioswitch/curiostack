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
package org.curioswitch.common.server.framework.auth.firebase;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.Authorizer;
import com.linecorp.armeria.server.auth.HttpAuthService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link HttpAuthService} that allows unauthenticated requests to still succeed for certain
 * paths. Forks authentication logic from {@code HttpAuthServiceImpl}.
 *
 * <p>TODO(choko): Remove after https://github.com/line/armeria/issues/506
 */
public class FirebaseAuthService extends HttpAuthService {

  /** Factory of {@link FirebaseAuthService}. */
  public static class Factory {
    private final FirebaseAuthConfig config;

    @Inject
    public Factory(FirebaseAuthConfig config) {
      this.config = config;
    }

    /**
     * Creates a new HTTP authorization {@link Service} decorator using the specified {@link
     * Authorizer}s.
     *
     * @param authorizers a list of {@link Authorizer}s.
     */
    public Function<Service<HttpRequest, HttpResponse>, HttpAuthService> newDecorator(
        Iterable<? extends Authorizer<HttpRequest>> authorizers) {
      return service -> new FirebaseAuthService(service, authorizers, config);
    }
  }

  private static final Logger logger = LogManager.getLogger();

  private final List<? extends Authorizer<HttpRequest>> authorizers;
  private final FirebaseAuthConfig config;

  /** Creates a new instance that provides HTTP authorization functionality to {@code delegate}. */
  private FirebaseAuthService(
      Service<HttpRequest, HttpResponse> delegate,
      Iterable<? extends Authorizer<HttpRequest>> authorizers,
      FirebaseAuthConfig config) {
    super(delegate);
    this.authorizers = ImmutableList.copyOf(authorizers);
    this.config = config;
  }

  // NOTE: Forked as is from HttpAuthServiceImpl
  @Override
  protected final CompletionStage<Boolean> authorize(HttpRequest req, ServiceRequestContext ctx) {
    CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
    for (Authorizer<HttpRequest> authorizer : authorizers) {
      result =
          result
              .exceptionally(
                  t -> {
                    logger.warn("Unexpected exception during authorization:", t);
                    return false;
                  })
              .thenComposeAsync(
                  previousResult -> {
                    if (previousResult) {
                      return CompletableFuture.completedFuture(true);
                    }
                    return authorizer.authorize(ctx, req);
                  },
                  ctx.contextAwareEventLoop());
    }
    return result;
  }

  @Override
  protected final HttpResponse onFailure(
      ServiceRequestContext ctx, HttpRequest req, @Nullable Throwable cause) throws Exception {
    if (!config.getIncludedPaths().isEmpty()) {
      if (config.getIncludedPaths().contains(ctx.path())) {
        return super.onFailure(ctx, req, cause);
      } else {
        return super.onSuccess(ctx, req);
      }
    }
    if (config.getExcludedPaths().contains(ctx.path())) {
      return super.onSuccess(ctx, req);
    }
    return super.onFailure(ctx, req, cause);
  }

  @Override
  public final String toString() {
    return MoreObjects.toStringHelper(this).addValue(authorizers).toString();
  }
}

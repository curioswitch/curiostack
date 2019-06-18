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

import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.ServerCacheControl;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.ServiceRequestContextWrapper;
import com.linecorp.armeria.server.SimpleDecoratingService;
import com.linecorp.armeria.server.composition.AbstractCompositeService;
import com.linecorp.armeria.server.composition.CompositeServiceEntry;
import com.linecorp.armeria.server.file.HttpFileBuilder;
import com.linecorp.armeria.server.file.HttpFileService;
import com.linecorp.armeria.server.file.HttpFileServiceBuilder;

/**
 * A {@link com.linecorp.armeria.server.Service} which serves a singlepage static site (SPA). All
 * requests to the static path (e.g., "/static/") will be resolved to a file in the classpath,
 * "sw.js", used to register service workers, will also be resolved to itself in the classpath, and
 * all other requests will resolve to "index.html" in the classpath for handling by the SPA.
 *
 * <p>The static site will automatically serve precompressed files if they are found, using the
 * conventions specified in {@link HttpFileServiceBuilder#serveCompressedFiles}.
 */
public class StaticSiteService extends AbstractCompositeService<HttpRequest, HttpResponse> {

  /**
   * Creates a new {@link StaticSiteService}.
   *
   * @param staticPath the URL path from which static resources will be served, e.g., "/static".
   * @param classpathRoot the root directory in the classpath to serve resources from.
   */
  public static StaticSiteService of(String staticPath, String classpathRoot) {
    HttpFileService staticFileService =
        HttpFileServiceBuilder.forClassPath(classpathRoot)
            .serveCompressedFiles(true)
            .cacheControl(ServerCacheControl.IMMUTABLE)
            .addHeader(HttpHeaderNames.VARY, "Accept-Encoding")
            .build();

    HttpService indexHtmlService =
        HttpFileBuilder.ofResource(classpathRoot + "/index.html")
            .cacheControl(ServerCacheControl.DISABLED)
            .build()
            .asService();

    TrailingSlashAddingService indexService =
        HttpFileServiceBuilder.forClassPath(classpathRoot)
            .serveCompressedFiles(true)
            .cacheControl(ServerCacheControl.DISABLED)
            .build()
            .orElse(indexHtmlService)
            .decorate(TrailingSlashAddingService::new);

    return new StaticSiteService(staticPath, staticFileService, indexService);
  }

  @SuppressWarnings("ConstructorInvokesOverridable")
  private StaticSiteService(
      String staticPath, HttpFileService fileService, TrailingSlashAddingService indexService) {
    super(
        CompositeServiceEntry.ofPrefix(staticPath, fileService),
        CompositeServiceEntry.ofCatchAll(indexService));
  }

  private static class TrailingSlashAddingService
      extends SimpleDecoratingService<HttpRequest, HttpResponse> {

    private TrailingSlashAddingService(Service<HttpRequest, HttpResponse> delegate) {
      super(delegate);
    }

    @Override
    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
      if (ctx.mappedPath().indexOf('.', ctx.mappedPath().lastIndexOf('/') + 1) != -1
          || ctx.mappedPath().charAt(ctx.mappedPath().length() - 1) == '/') {
        // A path that ends with '/' will be handled by HttpFileService correctly, and otherwise if
        // it has a '.' in the last path segment, assume it is a filename.
        return delegate().serve(ctx, req);
      }
      return delegate().serve(new ContextWrapper(ctx), req);
    }

    private static class ContextWrapper extends ServiceRequestContextWrapper {

      private final String indexPath;

      /** Creates a new instance. */
      private ContextWrapper(ServiceRequestContext delegate) {
        super(delegate);
        indexPath = delegate.mappedPath() + "/";
      }

      @Override
      public String mappedPath() {
        return indexPath;
      }

      @Override
      public String decodedMappedPath() {
        return indexPath;
      }
    }
  }
}

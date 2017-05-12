/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

import org.curioswitch.common.server.framework.immutables.CurioStyle;
import org.immutables.value.Value.Immutable;

/**
 * A definition of a {@link StaticSiteService} that should be registered to a {@link
 * com.linecorp.armeria.server.Server}. Provide it from a {@link dagger.Module} to allow automatic
 * registration.
 */
@Immutable
@CurioStyle
public interface StaticSiteServiceDefinition {

  class Builder extends ImmutableStaticSiteServiceDefinition.Builder {}

  /** The URL root to serve the site from. Defaults to "/". */
  default String urlRoot() {
    return "/";
  }

  /** The root directory in the classpath to serve resources from. */
  String classpathRoot();

  /**
   * The URL path under {@link #urlRoot} from which static resources will be served. Defaults to
   * "/static".
   */
  default String staticPath() {
    return "/static/";
  }
}

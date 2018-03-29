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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import org.curioswitch.common.server.framework.immutables.JavaBeanStyle;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

/** Configuration properties for a firebase authentication. */
@Immutable
@Modifiable
@JavaBeanStyle
public interface FirebaseAuthConfig {
  /** Base64-encoded firebase service account. */
  String getServiceAccountBase64();

  /** Firebase project id */
  String getProjectId();

  /** Whether to authenticate users with unverified email address. */
  boolean isAllowUnverifiedEmail();

  /**
   * A list of allowed Google-login domains. If non-empty, only tokens that authenticate to users
   * logged into Google with one of these domains will be allowed.
   */
  List<String> getAllowedGoogleDomains();

  /**
   * A list of paths that should allow unauthenticated requests. Unauthenticated requests to paths
   * in this list will be allowed. Only one of excluded or included paths can be set, not both.
   */
  List<String> getExcludedPaths();

  /**
   * A list of paths that must allow authenticated requests. Unauthenticated requests to paths not
   * in this list will be allowed. Only one of excluded or included paths can be set, not both.
   */
  List<String> getIncludedPaths();

  @Check
  default void check() {
    checkArgument(
        getExcludedPaths().isEmpty() || getIncludedPaths().isEmpty(),
        "Both excluded paths and included paths cannot be set at the same time.");
  }
}

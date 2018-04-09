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

package org.curioswitch.common.server.framework.auth.googleid;

import com.google.common.base.Strings;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.server.auth.OAuth2Token;
import io.netty.util.AsciiString;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO(choko): Remove after https://github.com/line/armeria/pull/1132/files
 *
 * <p>Extracts {@link OAuth2Token} from {@link HttpHeaders}.
 */
final class OAuth2TokenExtractor implements Function<HttpHeaders, OAuth2Token> {

  private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenExtractor.class);
  private static final Pattern AUTHORIZATION_HEADER_PATTERN =
      Pattern.compile("\\s*(?i)bearer\\s+(?<accessToken>\\S+)\\s*");

  private final AsciiString header;

  OAuth2TokenExtractor(AsciiString header) {
    this.header = header;
  }

  @Nullable
  @Override
  public OAuth2Token apply(HttpHeaders headers) {
    final String authorization = headers.get(header);
    if (Strings.isNullOrEmpty(authorization)) {
      return null;
    }

    final Matcher matcher = AUTHORIZATION_HEADER_PATTERN.matcher(authorization);
    if (!matcher.matches()) {
      logger.warn("Invalid authorization header: " + authorization);
      return null;
    }

    return OAuth2Token.of(matcher.group("accessToken"));
  }
}

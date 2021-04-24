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

package org.curioswitch.curiostack.gcloud.core.auth;

import com.google.auth.oauth2.UserCredentials;
import com.linecorp.armeria.client.WebClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.time.Clock;

class UserCredentialsAccessTokenProvider extends AbstractAccessTokenProvider {

  private static final String GRANT_TYPE = "refresh_token";

  private final ByteBuf refreshRequestContent;

  UserCredentialsAccessTokenProvider(
      WebClient googleAccountsClient, Clock clock, UserCredentials credentials) {
    super(googleAccountsClient, clock);
    refreshRequestContent = createRefreshRequestContent(credentials);
  }

  @Override
  ByteBuf refreshRequestContent(Type unused) {
    return refreshRequestContent.retainedDuplicate();
  }

  private static ByteBuf createRefreshRequestContent(UserCredentials credentials) {
    QueryStringEncoder formEncoder = new QueryStringEncoder("");
    formEncoder.addParam("client_id", credentials.getClientId());
    formEncoder.addParam("client_secret", credentials.getClientSecret());
    formEncoder.addParam("refresh_token", credentials.getRefreshToken());
    formEncoder.addParam("grant_type", GRANT_TYPE);
    String contentWithQuestionMark = formEncoder.toString();

    ByteBuf content = Unpooled.buffer(contentWithQuestionMark.length() - 1);
    ByteBufUtil.writeAscii(content, contentWithQuestionMark.substring(1));
    return content;
  }
}

/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.base.MoreObjects;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.RequestContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

class ServiceAccountAccessTokenProvider extends AbstractAccessTokenProvider {

  private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

  private static final String AUDIENCE = "https://www.googleapis.com/oauth2/v4/token";

  private final ServiceAccountCredentials credentials;

  ServiceAccountAccessTokenProvider(
      WebClient googleAccountsClient, Clock clock, ServiceAccountCredentials credentials) {
    super(googleAccountsClient, clock);
    this.credentials = credentials;
  }

  @Override
  ByteBuf refreshRequestContent(Type type) {
    long currentTimeMillis = clock().millis();
    String assertion = createAssertion(type, currentTimeMillis);
    QueryStringEncoder formEncoder = new QueryStringEncoder("");
    formEncoder.addParam("grant_type", GRANT_TYPE);
    formEncoder.addParam("assertion", assertion);
    String contentWithQuestionMark = formEncoder.toString();

    ByteBufAllocator alloc =
        RequestContext.mapCurrent(RequestContext::alloc, () -> PooledByteBufAllocator.DEFAULT);
    assert alloc != null;
    ByteBuf content = alloc.buffer(contentWithQuestionMark.length() - 1);
    ByteBufUtil.writeAscii(content, contentWithQuestionMark.substring(1));
    return content;
  }

  private String createAssertion(Type type, long currentTimeMillis) {
    JsonWebSignature.Header header = new JsonWebSignature.Header();
    header.setAlgorithm("RS256");
    header.setType("JWT");
    header.setKeyId(credentials.getPrivateKeyId());

    long currentTimeSecs = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis);

    JsonWebToken.Payload payload = new JsonWebToken.Payload();

    String serviceAccount =
        MoreObjects.firstNonNull(credentials.getServiceAccountUser(), credentials.getClientEmail());

    payload.setIssuer(serviceAccount);
    payload.setAudience(AUDIENCE);
    payload.setIssuedAtTimeSeconds(currentTimeSecs);
    payload.setExpirationTimeSeconds(currentTimeSecs + 3600);
    payload.setSubject(serviceAccount);
    payload.put(
        "scope",
        type == Type.ID_TOKEN
            ? credentials.getClientEmail()
            : String.join(" ", credentials.getScopes()));

    String assertion;
    try {
      assertion =
          JsonWebSignature.signUsingRsaSha256(
              credentials.getPrivateKey(), JacksonFactory.getDefaultInstance(), header, payload);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException(
          "Error signing service account access token request with private key.", e);
    }
    return assertion;
  }
}

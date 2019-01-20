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
package org.curioswitch.common.server.framework.auth.ssl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.Authorizer;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.logging.RequestLoggingContext;

public class SslAuthorizer implements Authorizer<HttpRequest> {

  private static final Logger logger = LogManager.getLogger();

  private final SslCommonNamesProvider commonNamesProvider;

  public SslAuthorizer(SslCommonNamesProvider commonNamesProvider) {
    this.commonNamesProvider = commonNamesProvider;
  }

  @Override
  public CompletionStage<Boolean> authorize(ServiceRequestContext ctx, HttpRequest unused) {
    checkNotNull(ctx.sslSession());
    final Certificate[] peerCerts;
    try {
      peerCerts = ctx.sslSession().getPeerCertificates();
    } catch (SSLPeerUnverifiedException e) {
      logger.warn("Could not verify peer.", e);
      return CompletableFuture.completedFuture(false);
    }
    if (peerCerts.length == 0) {
      logger.info("No peer certificates.");
      return CompletableFuture.completedFuture(false);
    }
    String name =
        ((X509Certificate) peerCerts[0])
            .getSubjectX500Principal()
            .getName()
            .substring("CN=".length());

    RequestLoggingContext.put(ctx, "sslCommonName", name);

    boolean authorized = commonNamesProvider.get().contains(name);
    if (authorized) {
      return CompletableFuture.completedFuture(true);
    } else {
      logger.info("Rejecting SSL client: " + name);
      return CompletableFuture.completedFuture(false);
    }
  }
}

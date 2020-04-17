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
package org.curioswitch.common.server.framework.auth.iam;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.collect.ImmutableList;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.auth.Authorizer;
import com.linecorp.armeria.server.auth.OAuth2Token;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

public class IamAuthorizer implements Authorizer<OAuth2Token> {

  private static final List<String> PERMISSIONS = ImmutableList.of("iam.serviceAccounts.actAs");

  private final IamPermissionChecker checker;
  private final String serviceAccount;

  @Inject
  public IamAuthorizer(IamPermissionChecker checker, Credentials serverCredentials) {
    checkArgument(
        serverCredentials instanceof ServiceAccountCredentials,
        "IAM authentication only works with service account credentials.");
    this.checker = checker;
    ServiceAccountCredentials creds = (ServiceAccountCredentials) serverCredentials;
    serviceAccount =
        Objects.requireNonNullElse(creds.getServiceAccountUser(), creds.getClientEmail());
  }

  @Override
  public CompletionStage<Boolean> authorize(ServiceRequestContext ctx, OAuth2Token data) {
    return checker.test(data.accessToken(), serviceAccount, PERMISSIONS);
  }
}

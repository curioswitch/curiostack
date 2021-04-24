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

package org.curioswitch.common.server.framework.config;

import java.util.List;
import org.curioswitch.common.server.framework.immutables.JavaBeanStyle;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

/** General configuration properties for the server. */
@Immutable
@Modifiable
@JavaBeanStyle
public interface ServerConfig {

  /** Port to listen on. Server will listen for HTTPS on this port. Defaults to 8080. */
  int getPort();

  /**
   * Whether the server should generate a self-signed SSL certificate for the HTTPs port. This
   * should only be enabled for local development.
   */
  boolean isGenerateSelfSignedCertificate();

  /**
   * Whether clients should have SSL certificate verification disabled. This should only be enabled
   * for local development.
   */
  boolean isDisableClientCertificateVerification();

  /**
   * Whether backend service SSL certificates should be verified. This should only be enabled for
   * local development.
   */
  boolean isDisableServerCertificateVerification();

  /** Path to the file containing the TLS certificate for this server. */
  String getTlsCertificatePath();

  /** Path to the file containing the private key for the TLS certificate for this server. */
  String getTlsPrivateKeyPath();

  /**
   * Path to the file containing the TLS certificate for client requests. If unset,
   * tlsCertificatePath is used.
   */
  String getClientTlsCertificatePath();

  /**
   * Path to the file containing the private key for client requests. If unset, tlsPrivateKeyPath is
   * used.
   */
  String getClientTlsPrivateKeyPath();

  /**
   * Path to the file containing the certificate of the CA that issues server/client certs in this
   * system.
   */
  String getCaCertificatePath();

  /**
   * Path to an additional CA certificate, necessary for migrating CAs as two CAs need to be trusted
   * at the same time.
   */
  String getAdditionalCaCertificatePath();

  /** The path to service gRPC APIs on, defaults to /api. */
  String getGrpcPath();

  /**
   * Whether the {@link io.grpc.protobuf.services.ProtoReflectionService} should be added to the
   * server to enable discovery of bound {@link com.linecorp.armeria.server.grpc.GrpcService}s. The
   * "grpc.reflection.v1alpha.ServerReflection/*" path should be blocked from external traffic when
   * enabling this service. If it is difficult to block the service, this should be disabled
   * instead.
   */
  boolean isDisableGrpcServiceDiscovery();

  /**
   * Whether the {@link com.linecorp.armeria.server.docs.DocService} is disabled in the server. It
   * is recommended to leave it enabled, and this option has mainly been added temporarily as a
   * workaround for https://github.com/line/armeria/pull/592.
   */
  boolean isDisableDocService();

  /**
   * Path to file containing rpc acl configuration. If empty, features using rpc acl will be
   * disabled, including:
   *
   * <ul>
   *   <li>{@link org.curioswitch.common.server.framework.auth.ssl.SslAuthorizer}
   * </ul>
   */
  String getRpcAclsPath();

  /**
   * Whether authorization using SSL client certificates should be disabled. This should generally
   * only be set to {@code true} for services used from browsers.
   */
  boolean isDisableSslAuthorization();

  /** Whether to enable Google Identity-Aware Proxy token verification. */
  boolean isEnableIapAuthorization();

  /**
   * List of IP filtering rules, as IP Addresses with subnet range (e.g., 121.121.0.0/16). If
   * non-empty, only requests that match these rules will be allowed to access the server.
   */
  List<String> getIpFilterRules();

  /**
   * Sets whether IP filter rules should only be applied to internal services. If not set, IP filter
   * rules are applied to all requests.
   */
  boolean getIpFilterInternalOnly();

  /**
   * Sets whether to shutdown gracefully, by first disabling health check and then wait some time
   * for requests to go away before shutting down. This should always be set in non-local
   * deployments.
   */
  boolean getEnableGracefulShutdown();

  /**
   * Whether EDNS should be disabled in clients. This is required when connecting to a server in an
   * environment with a DNS server that doesn't support EDNS.
   */
  boolean getDisableEdns();
}

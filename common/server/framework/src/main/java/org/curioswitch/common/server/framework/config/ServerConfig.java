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

package org.curioswitch.common.server.framework.config;

import org.curioswitch.common.server.framework.immutables.JavaBeanStyle;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

/** General configuration properties for the server. */
@Immutable
@Modifiable
@JavaBeanStyle
public interface ServerConfig {

  /**
   * Whether the server should generate a self-signed SSL certificate for the HTTPs port. This
   * should only be enabled for local development.
   */
  boolean isGenerateSelfSignedCertificate();

  /** Path to the file containing the TLS certificate for this server. */
  String getTlsCertificatePath();

  /** Path to the file containing the private key for the TLS certificate for this server. */
  String getTlsPrivateKeyPath();

  /**
   * Whether the {@link io.grpc.protobuf.services.ProtoReflectionService} should be added to the
   * server to enable discovery of bound {@link com.linecorp.armeria.server.grpc.GrpcService}s. The
   * "grpc.reflection.v1alpha.ServerReflection/*" path should be blocked from external traffic when
   * enabling this service. If it is difficult to block the service, this should be disabled
   * instead.
   */
  boolean isDisableGrpcServiceDiscovery();
}

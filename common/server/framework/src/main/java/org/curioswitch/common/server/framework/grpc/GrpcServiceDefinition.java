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

package org.curioswitch.common.server.framework.grpc;

import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder;
import io.grpc.BindableService;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.curioswitch.common.server.framework.immutables.CurioStyle;
import org.immutables.value.Value.Immutable;

/**
 * A definition of a gRPC {@link BindableService} that should be registered to a {@link
 * com.linecorp.armeria.server.Server}. A {@link BindableService} can be provided directly to
 * register simple services, but {@link GrpcServiceDefinition} should be used when needing to set up
 * custom settings like decoerators.
 */
@Immutable
@CurioStyle
public interface GrpcServiceDefinition {

  Consumer<GrpcServiceBuilder> NO_OP = (unused) -> {};

  class Builder extends ImmutableGrpcServiceDefinition.Builder {}

  /** The gRPC services to bind. */
  List<BindableService> services();

  /** The decorator to be applied to the service. */
  Function<? super HttpService, ? extends HttpService> decorator();

  /** The URL path to bind the service to. */
  default String path() {
    return "/api/";
  }

  /**
   * A {@link Consumer} to customize settings of the {@link GrpcServiceBuilder} this service is
   * bound to.
   */
  default Consumer<GrpcServiceBuilder> customizer() {
    return NO_OP;
  }
}

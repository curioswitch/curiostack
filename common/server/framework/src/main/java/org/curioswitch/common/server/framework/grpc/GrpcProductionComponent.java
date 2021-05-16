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

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An interface for {@link dagger.producers.ProductionComponent}s that handle a gRPC service method.
 * These components always only have a single request and single asynchronous response.
 *
 * @param <Resp> The response type of the service.
 * @deprecated Define the {@code ListenableFuture<Resp> execute()} in your components. When using
 *     {@link dagger.BindsInstance}, the boilerplate savings of this interface are too low to
 *     warrant it.
 */
@Deprecated
public interface GrpcProductionComponent<Resp> {

  /** Execute the graph to compute the response. */
  ListenableFuture<Resp> execute();

  /**
   * An interface for {@link dagger.producers.ProductionComponent.Builder}s that construct {@link
   * GrpcProductionComponent}. These always only take a graph (which is constructed with the
   * request) and return the component.
   *
   * @param <G> The graph type.
   * @param <C> The component type.
   * @deprecated Use {@link dagger.BindsInstance} instead of passing the {@link
   *     dagger.producers.ProducerModule} into the component. All modules should be abstract.
   */
  @Deprecated
  interface GrpcProductionComponentBuilder<
      G, C extends GrpcProductionComponent, Self extends GrpcProductionComponentBuilder> {

    /** Sets the graph to use for this component. */
    Self graph(G graph);

    /** Builds and returns the component. */
    C build();
  }
}

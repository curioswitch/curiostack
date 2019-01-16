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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import javax.inject.Provider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.curioswitch.common.server.framework.grpc.GrpcProductionComponent.GrpcProductionComponentBuilder;

/** A utility for using gRPC with producer graphs. */
public final class GrpcGraphUtil {

  /**
   * Wires the result of a {@link ListenableFuture} to the response handling methods of a {@link
   * StreamObserver} for a {@link
   * org.curioswitch.common.server.framework.grpc.GrpcProductionComponent} which takes no arguments.
   */
  public static <Resp extends Message, C extends GrpcProductionComponent<Resp>> void unary(
      StreamObserver<Resp> observer, Provider<C> component) {
    Futures.addCallback(
        component.get().execute(),
        new UnaryStreamObserverCallback<>(observer),
        MoreExecutors.directExecutor());
  }

  /**
   * Invokes a unary {@link org.curioswitch.common.server.framework.grpc.GrpcProductionComponent}
   * with the provided request and graph factory.
   */
  @SuppressWarnings("InconsistentOverloads")
  public static <
          G,
          Resp extends Message,
          C extends GrpcProductionComponent<Resp>,
          B extends GrpcProductionComponentBuilder<G, C, B>>
      void unary(G graph, StreamObserver<Resp> observer, Provider<B> componentBuilder) {
    Futures.addCallback(
        componentBuilder.get().graph(graph).build().execute(),
        new UnaryStreamObserverCallback<>(observer),
        MoreExecutors.directExecutor());
  }

  private static class UnaryStreamObserverCallback<T> implements FutureCallback<T> {

    private final StreamObserver<T> observer;

    private UnaryStreamObserverCallback(StreamObserver<T> observer) {
      this.observer = observer;
    }

    @Override
    public void onSuccess(@Nullable T result) {
      observer.onNext(result);
      observer.onCompleted();
    }

    @Override
    public void onFailure(Throwable t) {
      observer.onError(t);
    }
  }

  private GrpcGraphUtil() {}
}

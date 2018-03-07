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
package org.curioswitch.common.server.framework.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.futures.FuturesExtra;
import io.grpc.stub.StreamObserver;

/** A utility for using gRPC with producer graphs. */
public final class GrpcGraphUtil {

  /**
   * Wires the result of a {@link ListenableFuture} to the response handling methods of a {@link
   * StreamObserver}.
   */
  public static <T> void unary(ListenableFuture<T> graphFuture, StreamObserver<T> observer) {
    FuturesExtra.addCallback(
        graphFuture,
        response -> {
          observer.onNext(response);
          observer.onCompleted();
        },
        observer::onError);
  }

  private GrpcGraphUtil() {}
}

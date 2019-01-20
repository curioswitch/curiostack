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
package org.curioswitch.common.testing.grpc;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public final class GrpcTestUtil {

  public static <I extends Message, O extends Message, S extends StreamObserver<O>>
      CompletableFuture<O> executeUnary(BiConsumer<I, S> method, I request) {
    CompletableFuture<O> future = new CompletableFuture<>();
    AtomicReference<O> message = new AtomicReference<>();
    @SuppressWarnings("unchecked")
    S streamObserver =
        (S)
            new StreamObserver<O>() {
              @Override
              public void onNext(O value) {
                if (message.get() != null) {
                  future.completeExceptionally(
                      new IllegalStateException("Got second message for unary."));
                }
                message.set(value);
              }

              @Override
              public void onError(Throwable t) {
                future.completeExceptionally(t);
              }

              @Override
              public void onCompleted() {
                future.complete(message.get());
              }
            };
    method.accept(request, streamObserver);
    return future;
  }

  private GrpcTestUtil() {}
}

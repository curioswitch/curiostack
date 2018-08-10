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

package org.curioswitch.common.testing.assertj;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.getUnchecked;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.util.function.Consumer;
import org.assertj.core.api.ObjectAssert;

public class ListenableFutureAssert<ACTUAL> extends ObjectAssert<ListenableFuture<ACTUAL>> {

  ListenableFutureAssert(ListenableFuture<ACTUAL> future) {
    super(future);
  }

  public ListenableFutureAssert<ACTUAL> completesWithValue(ACTUAL value) {
    if (value instanceof Message) {
      Message resolved = (Message) getUnchecked(actual);
      assertThat(resolved).isEqualTo(value);
    }
    assertThat(getUnchecked(actual)).isEqualTo(value);
    return this;
  }

  public ListenableFutureAssert<ACTUAL> completesWithValueSatisfying(
      Consumer<ACTUAL> requirements) {
    assertThat(getUnchecked(actual)).satisfies(requirements);
    return this;
  }

  public ListenableFutureAssert<ACTUAL> completesWithValueMatchingSnapshot() {
    assertThat(getUnchecked(actual)).matchesSnapshot();
    return this;
  }

  public <T> ListenableFutureAssert<ACTUAL> failsWithInstanceOfSatisfying(
      Class<T> type, Consumer<T> requirements) {
    checkNotNull(
        requirements, "The Consumer<T> expressing the assertions requirements must not be null");
    Throwable t = catchThrowable(() -> getUnchecked(actual));
    assertThat(t.getCause()).isInstanceOfSatisfying(type, requirements);
    return this;
  }

  public ListenableFutureAssert<ACTUAL> failsWithGrpcStatus(Status status) {
    checkNotNull(status, "status");
    assertThat(getFailureGrpcStatus()).isEqualTo(status);
    return this;
  }

  public ListenableFutureAssert<ACTUAL> failsWithGrpcStatusCode(Status.Code code) {
    checkNotNull(code, "code");
    assertThat(getFailureGrpcStatus().getCode()).isEqualTo(code);
    return this;
  }

  private Status getFailureGrpcStatus() {
    Throwable t = catchThrowable(() -> getUnchecked(actual));
    Throwable cause = t.getCause();
    if (cause instanceof StatusRuntimeException) {
      return ((StatusRuntimeException) cause).getStatus();
    } else if (cause instanceof StatusException) {
      return ((StatusException) cause).getStatus();
    } else {
      // Could throw AssertionError, but use assertj for consistent error messages. The following
      // is guaranteed to throw.
      assertThat(cause).isInstanceOfAny(StatusException.class, StatusRuntimeException.class);
      throw new IllegalStateException("Can't reach here.");
    }
  }
}

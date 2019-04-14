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

package org.curioswitch.common.server.framework.server;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An interface which can be implemented and provided to delay shutdown of the server. Health
 * check will become unhealthy when the server receives a termination signal but the server will
 * not begin shutdown until all {@link ServerShutDownDelayer}s have indicated they are ready.
 */
public interface ServerShutDownDelayer {

  /**
   * Returns a future which must be completed to indicate the server will be shutdown. This method
   * will be called when the server receives a shutdown signal. The server will actually begin to
   * shutdown when all {@link ServerShutDownDelayer} have completed their returned future.
   * Implementations should only complete the future successfully - exceptional completion will
   * still trigger shutdown but will not be handled or logged in anyway.
   *
   * <p>There is no timeout on completion of this future - it is recommended an external timeout,
   * such as a Kubernetes shutdown delay, is used to ensure the server isn't stuck forever.
   */
  ListenableFuture<Void> readyForShutdown();
}

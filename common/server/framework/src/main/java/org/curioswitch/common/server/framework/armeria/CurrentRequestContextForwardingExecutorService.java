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

package org.curioswitch.common.server.framework.armeria;

import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.linecorp.armeria.common.RequestContext;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * A {@link Executor} that will run tasks on a delegate, making callbacks context-aware when a
 * {@link RequestContext} is available.
 */
public class CurrentRequestContextForwardingExecutorService extends ForwardingListeningExecutorService {

  private final ListeningExecutorService delegate;

  public CurrentRequestContextForwardingExecutorService(ExecutorService delegate) {
    this.delegate = MoreExecutors.listeningDecorator(delegate);
  }

  @Override
  public void execute(Runnable command) {
    RequestContext ctx = RequestContext.mapCurrent(Function.identity(), null);
    if (ctx != null) {
      delegate.execute(ctx.makeContextAware(command));
    } else {
      delegate.execute(command);
    }
  }

  @Override
  protected ListeningExecutorService delegate() {
    return delegate;
  }
}

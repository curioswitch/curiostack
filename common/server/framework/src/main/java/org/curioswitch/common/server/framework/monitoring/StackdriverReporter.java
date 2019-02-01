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
package org.curioswitch.common.server.framework.monitoring;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.devtools.cloudtrace.v2.BatchWriteSpansRequest;
import com.google.devtools.cloudtrace.v2.TraceServiceGrpc.TraceServiceFutureStub;
import com.google.protobuf.Empty;
import dagger.Lazy;
import java.io.Flushable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.config.MonitoringConfig;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscCompoundQueue;
import zipkin2.Span;
import zipkin2.reporter.Reporter;
import zipkin2.translation.stackdriver.SpanTranslator;

@Singleton
public class StackdriverReporter implements Reporter<Span>, Flushable, AutoCloseable {

  private static final Logger logger = LogManager.getLogger();

  private final Lazy<TraceServiceFutureStub> traceServiceClient;
  private final MessagePassingQueue<Span> queue;
  private final String projectId;

  @Inject
  public StackdriverReporter(
      Lazy<TraceServiceFutureStub> traceServiceClient, MonitoringConfig config) {
    this.traceServiceClient = traceServiceClient;
    queue = new MpscCompoundQueue<>(config.getTraceQueueSize());
    projectId = config.getStackdriverProjectId();
  }

  @Override
  public void report(Span span) {
    queue.relaxedOffer(span);
  }

  @Override
  public void flush() {
    List<Span> spans = new ArrayList<>(queue.size());
    queue.drain(spans::add);
    if (spans.isEmpty()) {
      return;
    }

    List<com.google.devtools.cloudtrace.v2.Span> translated =
        SpanTranslator.translate(projectId, spans);

    BatchWriteSpansRequest request =
        BatchWriteSpansRequest.newBuilder()
            .setName("projects/" + projectId)
            .addAllSpans(translated)
            .build();

    Futures.addCallback(
        traceServiceClient.get().batchWriteSpans(request),
        new FutureCallback<Empty>() {
          @Override
          public void onFailure(Throwable t) {
            logger.warn("Error reporting traces.", t);
          }

          @Override
          public void onSuccess(Empty result) {
            logger.trace("Successfully reported traces.");
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public void close() throws Exception {
    flush();
  }
}

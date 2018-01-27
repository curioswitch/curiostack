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

package org.curioswitch.common.server.framework.monitoring;

import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.zipkin.translation.TraceTranslator;
import com.google.devtools.cloudtrace.v1.PatchTracesRequest;
import com.google.devtools.cloudtrace.v1.Traces;
import com.google.protobuf.Empty;
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

@Singleton
public class StackdriverReporter implements Reporter<Span>, Flushable, AutoCloseable {

  private static final Logger logger = LogManager.getLogger();

  private final TraceServiceClient traceServiceClient;
  private final MessagePassingQueue<Span> queue;
  private final TraceTranslator translator;
  private final String projectId;

  @Inject
  public StackdriverReporter(TraceServiceClient traceServiceClient, MonitoringConfig config) {
    this.traceServiceClient = traceServiceClient;
    queue = new MpscCompoundQueue<>(config.getTraceQueueSize());
    translator = new TraceTranslator(config.getStackdriverProjectId());
    projectId = config.getStackdriverProjectId();
  }

  @Override
  public void report(Span span) {
    queue.relaxedOffer(span);
  }

  @Override
  public void flush() {
    List<Span> spans = new ArrayList<>();
    queue.drain(spans::add);
    if (spans.isEmpty()) {
      return;
    }
    PatchTracesRequest request =
        PatchTracesRequest.newBuilder()
            .setProjectId(projectId)
            .setTraces(Traces.newBuilder().addAllTraces(translator.translateSpans(spans)))
            .build();
    ApiFutures.addCallback(
        traceServiceClient.patchTracesCallable().futureCall(request),
        new ApiFutureCallback<Empty>() {
          @Override
          public void onFailure(Throwable t) {
            logger.warn("Error reporting traces.", t);
          }

          @Override
          public void onSuccess(Empty result) {
            logger.info("Successfully reported traces.");
          }
        });
  }

  @Override
  public void close() throws Exception {
    flush();
  }
}

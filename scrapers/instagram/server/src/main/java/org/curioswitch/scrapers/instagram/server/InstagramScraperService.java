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

package org.curioswitch.scrapers.instagram.server;

import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.grpc.GrpcGraphUtil;
import org.curioswitch.scrapers.instagram.api.InstagramScraperServiceGrpc.InstagramScraperServiceImplBase;
import org.curioswitch.scrapers.instagram.api.ScrapeLocationsRequest;
import org.curioswitch.scrapers.instagram.api.ScrapeLocationsResponse;
import org.curioswitch.scrapers.instagram.server.locations.ScrapeLocationsGraph;

@Singleton
class InstagramScraperService extends InstagramScraperServiceImplBase {

  private final Provider<ScrapeLocationsGraph.Component.Builder> scrapeLocationsGraph;

  @Inject
  InstagramScraperService(Provider<ScrapeLocationsGraph.Component.Builder> scrapeLocationsGraph) {
    this.scrapeLocationsGraph = scrapeLocationsGraph;
  }

  @Override
  public void scrapeLocations(
      ScrapeLocationsRequest request, StreamObserver<ScrapeLocationsResponse> responseObserver) {
    GrpcGraphUtil.unary(new ScrapeLocationsGraph(request), responseObserver, scrapeLocationsGraph);
  }
}

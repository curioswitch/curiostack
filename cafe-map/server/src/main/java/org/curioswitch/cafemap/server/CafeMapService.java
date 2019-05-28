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

package org.curioswitch.cafemap.server;

import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.curioswitch.cafemap.api.CafeMapServiceGrpc.CafeMapServiceImplBase;
import org.curioswitch.cafemap.api.GetPlaceRequest;
import org.curioswitch.cafemap.api.GetPlaceResponse;
import org.curioswitch.cafemap.api.GetPlacesRequest;
import org.curioswitch.cafemap.api.GetPlacesResponse;
import org.curioswitch.cafemap.api.ListLandmarksRequest;
import org.curioswitch.cafemap.api.ListLandmarksResponse;
import org.curioswitch.cafemap.server.places.GetPlaceGraph;
import org.curioswitch.cafemap.server.places.GetPlacesGraph;
import org.curioswitch.cafemap.server.places.ListLandmarksGraph;
import org.curioswitch.common.server.framework.grpc.GrpcGraphUtil;

@Singleton
public class CafeMapService extends CafeMapServiceImplBase {

  private final Provider<GetPlacesGraph.Component.Builder> getPlacesGraph;
  private final Provider<GetPlaceGraph.Component.Builder> getPlaceGraph;
  private final Provider<ListLandmarksGraph.Component.Builder> listLandmarksGraph;

  @Inject
  CafeMapService(
      Provider<GetPlacesGraph.Component.Builder> getPlacesGraph,
      Provider<GetPlaceGraph.Component.Builder> getPlaceGraph,
      Provider<ListLandmarksGraph.Component.Builder> listLandmarksGraph) {
    this.getPlacesGraph = getPlacesGraph;
    this.getPlaceGraph = getPlaceGraph;
    this.listLandmarksGraph = listLandmarksGraph;
  }

  @Override
  public void getPlaces(
      GetPlacesRequest request, StreamObserver<GetPlacesResponse> responseObserver) {
    GrpcGraphUtil.unary(new GetPlacesGraph(request), responseObserver, getPlacesGraph);
  }

  @Override
  public void getPlace(GetPlaceRequest request, StreamObserver<GetPlaceResponse> responseObserver) {
    GrpcGraphUtil.unary(new GetPlaceGraph(request), responseObserver, getPlaceGraph);
  }

  @Override
  public void listLandmarks(
      ListLandmarksRequest request, StreamObserver<ListLandmarksResponse> responseObserver) {
    GrpcGraphUtil.unary(new ListLandmarksGraph(request), responseObserver, listLandmarksGraph);
  }
}

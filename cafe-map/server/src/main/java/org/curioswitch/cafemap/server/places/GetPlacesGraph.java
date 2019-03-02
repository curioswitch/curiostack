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

package org.curioswitch.cafemap.server.places;

import static org.curioswitch.database.cafemapdb.tables.Place.PLACE;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import dagger.producers.ProductionSubcomponent;
import java.util.List;
import org.curioswitch.cafemap.api.GetPlacesRequest;
import org.curioswitch.cafemap.api.GetPlacesResponse;
import org.curioswitch.cafemap.api.LatLng;
import org.curioswitch.common.server.framework.database.ForDatabase;
import org.curioswitch.common.server.framework.grpc.GrpcProductionComponent;
import org.curioswitch.database.cafemapdb.tables.pojos.Place;
import org.jooq.DSLContext;

@ProducerModule
public class GetPlacesGraph {

  @ProductionSubcomponent(modules = GetPlacesGraph.class)
  public interface Component extends GrpcProductionComponent<GetPlacesResponse> {
    @ProductionSubcomponent.Builder
    interface Builder extends GrpcProductionComponentBuilder<GetPlacesGraph, Component, Builder> {}
  }

  private final GetPlacesRequest request;

  public GetPlacesGraph(GetPlacesRequest request) {
    this.request = request;
  }

  @Produces
  GetPlacesRequest request() {
    return request;
  }

  @Produces
  ListenableFuture<List<Place>> fetchPlaces(
      DSLContext cafemapdb, @ForDatabase ListeningExecutorService dbExecutor) {
    return dbExecutor.submit(() -> cafemapdb.selectFrom(PLACE).fetchInto(Place.class));
  }

  @Produces
  static GetPlacesResponse response(List<Place> places) {
    return GetPlacesResponse.newBuilder()
        .addAllPlace(places.stream().map(GetPlacesGraph::convertPlace)::iterator)
        .build();
  }

  private static org.curioswitch.cafemap.api.Place convertPlace(Place place) {
    return org.curioswitch.cafemap.api.Place.newBuilder()
        .setName(place.getName())
        .setPosition(
            LatLng.newBuilder()
                .setLatitude(place.getLatitude())
                .setLongitude(place.getLongitude())
                .build())
        .setInstagramId(Strings.nullToEmpty(place.getInstagramId()))
        .build();
  }
}

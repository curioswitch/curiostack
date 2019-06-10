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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.curioswitch.database.cafemapdb.tables.Place.PLACE;

import com.google.common.collect.Streams;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;
import com.google.common.geometry.S2RegionCoverer;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dagger.BindsInstance;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import dagger.producers.ProductionSubcomponent;
import java.util.List;
import org.curioswitch.cafemap.api.GetPlacesRequest;
import org.curioswitch.cafemap.api.GetPlacesResponse;
import org.curioswitch.cafemap.api.ListLandmarksRequest;
import org.curioswitch.cafemap.server.util.S2Util;
import org.curioswitch.common.server.framework.database.ForDatabase;
import org.curioswitch.common.server.framework.grpc.Unvalidated;
import org.curioswitch.database.cafemapdb.tables.pojos.Place;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;

@ProducerModule
public abstract class GetPlacesGraph {

  @ProductionSubcomponent(modules = GetPlacesGraph.class)
  public interface Component {
    ListenableFuture<GetPlacesResponse> execute();

    @ProductionSubcomponent.Builder
    interface Builder {
      Builder setRequest(@BindsInstance @Unvalidated GetPlacesRequest request);

      Component build();
    }
  }

  @Produces
  static GetPlacesRequest validateRequest(@Unvalidated GetPlacesRequest request) {
    checkArgument(request.getViewport().hasNorthEast(), "viewport.north_east is required");
    checkArgument(request.getViewport().hasSouthWest(), "viewport.south_west is required");
    return request;
  }

  @Produces
  static S2LatLngRect viewport(ListLandmarksRequest request) {
    return S2Util.convertFromLatLngBounds(request.getViewport());
  }

  @Produces
  static ListenableFuture<List<Place>> fetchPlaces(
      S2LatLngRect viewport, DSLContext db, @ForDatabase ListeningExecutorService dbExecutor) {
    var coverer = new S2RegionCoverer();
    var coveredCells = coverer.getCovering(viewport);

    Condition locationCondition =
        DSL.or(
            Streams.stream(coveredCells)
                .map(
                    cell ->
                        PLACE
                            .S2_CELL
                            .ge(ULong.valueOf(cell.rangeMin().id()))
                            .and(PLACE.S2_CELL.le(ULong.valueOf(cell.rangeMax().id()))))
                .collect(toImmutableList()));

    return dbExecutor.submit(
        () -> db.selectFrom(PLACE).where(DSL.or(locationCondition)).fetchInto(Place.class));
  }

  @Produces
  static GetPlacesResponse response(List<Place> places, S2LatLngRect viewport) {
    return GetPlacesResponse.newBuilder()
        .addAllPlace(
            places.stream()
                    .filter(
                        place ->
                            viewport.contains(
                                S2LatLng.fromDegrees(place.getLatitude(), place.getLongitude())))
                    .map(PlaceUtil::convertPlace)
                ::iterator)
        .build();
  }

  private GetPlacesGraph() {}
}

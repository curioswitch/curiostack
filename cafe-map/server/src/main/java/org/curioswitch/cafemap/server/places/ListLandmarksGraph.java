/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.curioswitch.database.cafemapdb.tables.Landmark.LANDMARK;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;
import com.google.common.geometry.S2RegionCoverer;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.maps.GeoApiContext;
import com.google.maps.NearbySearchRequest;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import dagger.BindsInstance;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import dagger.producers.ProductionSubcomponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.curioswitch.cafemap.api.Landmark.Type;
import org.curioswitch.cafemap.api.ListLandmarksRequest;
import org.curioswitch.cafemap.api.ListLandmarksResponse;
import org.curioswitch.cafemap.server.util.S2Util;
import org.curioswitch.common.server.framework.database.ForDatabase;
import org.curioswitch.common.server.framework.grpc.Unvalidated;
import org.curioswitch.database.cafemapdb.tables.pojos.Landmark;
import org.curioswitch.database.cafemapdb.tables.records.LandmarkRecord;
import org.curioswitch.gcloud.mapsservices.CallbackListenableFuture;
import org.jooq.DSLContext;
import org.jooq.types.ULong;

@ProducerModule
public abstract class ListLandmarksGraph {

  @ProductionSubcomponent(modules = ListLandmarksGraph.class)
  public interface Component {
    ListenableFuture<ListLandmarksResponse> execute();

    @ProductionSubcomponent.Builder
    interface Builder {
      Builder setRequest(@BindsInstance @Unvalidated ListLandmarksRequest request);

      Component build();
    }
  }

  private static final PlacesSearchResult[] EMPTY = new PlacesSearchResult[0];

  @Produces
  static ListLandmarksRequest validateRequest(@Unvalidated ListLandmarksRequest request) {
    checkArgument(request.getViewport().hasNorthEast(), "viewport.north_east is required");
    checkArgument(request.getViewport().hasSouthWest(), "viewport.south_west is required");
    return request;
  }

  @Produces
  static S2LatLngRect viewport(ListLandmarksRequest request) {
    return S2Util.convertFromLatLngBounds(request.getViewport());
  }

  @Produces
  static S2CellUnion coveredCells(S2LatLngRect viewport) {
    var coverer = new S2RegionCoverer();
    return coverer.getCovering(viewport);
  }

  @Produces
  static ListenableFuture<List<List<Landmark>>> fetchDbLandmarks(
      S2CellUnion coveredCells, DSLContext db, @ForDatabase ListeningExecutorService dbExecutor) {
    return Futures.successfulAsList(
        Streams.stream(coveredCells)
            .map(
                cell ->
                    dbExecutor.submit(
                        () ->
                            db.selectFrom(LANDMARK)
                                .where(
                                    LANDMARK
                                        .S2_CELL
                                        .ge(ULong.valueOf(cell.rangeMin().id()))
                                        .and(
                                            LANDMARK.S2_CELL.le(
                                                ULong.valueOf(cell.rangeMax().id()))))
                                .fetchInto(Landmark.class)))
            .collect(toImmutableList()));
  }

  @Produces
  static ListenableFuture<PlacesSearchResponse> maybeSearchForLandmarks(
      S2LatLngRect viewport,
      GeoApiContext geoApiContext,
      S2CellUnion coveredCells,
      List<List<Landmark>> dbLandmarks) {
    // We find the first cell that is missing landmarks in the viewport to use as the center when
    // making the search request if needed.
    S2CellId firstCellMissingLandmarks =
        Streams.zip(
                Streams.stream(coveredCells),
                dbLandmarks.stream(),
                (cell, cellLandmarks) -> {
                  if (cellLandmarks != null && !cellLandmarks.isEmpty()) {
                    return S2CellId.none();
                  }
                  return cell;
                })
            .filter(S2CellId::isValid)
            .findFirst()
            .orElse(S2CellId.none());

    if (firstCellMissingLandmarks.isValid()) {
      // At least one cell was covered - for now don't search again if any cells are covered.
      // For better logic, we will need to keep track of searched viewports some way..
      var response = new PlacesSearchResponse();
      response.results = EMPTY;
      return immediateFuture(response);
    }

    S2LatLng location = viewport.getCenter();
    int radius = (int) location.getEarthDistance(viewport.lo());

    CallbackListenableFuture<PlacesSearchResponse> callback = new CallbackListenableFuture<>();
    new NearbySearchRequest(geoApiContext)
        .location(new LatLng(location.latDegrees(), location.lngDegrees()))
        .radius(radius)
        .type(PlaceType.PARK)
        .setCallback(callback);
    return callback;
  }

  @Produces
  static ListenableFuture<List<Landmark>> writeNewLandmarksToDb(
      PlacesSearchResponse searchResponse,
      List<List<Landmark>> dbLandmarks,
      DSLContext cafemapdb,
      @ForDatabase ListeningExecutorService dbExecutor) {
    if (searchResponse.results.length == 0) {
      return immediateFuture(ImmutableList.of());
    }

    Set<String> dbPlaceIds =
        dbLandmarks.stream()
            .flatMap(List::stream)
            .map(Landmark::getGooglePlaceId)
            .collect(toImmutableSet());

    List<LandmarkRecord> newLandmarks =
        Arrays.stream(searchResponse.results)
            .filter(result -> !dbPlaceIds.contains(result.placeId))
            .map(
                result ->
                    new LandmarkRecord()
                        .setGooglePlaceId(result.placeId)
                        .setType("park")
                        .setS2Cell(
                            ULong.valueOf(
                                S2CellId.fromLatLng(
                                        S2Util.convertFromLatLng(result.geometry.location))
                                    .id())))
            .collect(toImmutableList());

    return dbExecutor.submit(
        () -> {
          int[] numInserted = cafemapdb.batchStore(newLandmarks).execute();
          return newLandmarks.stream().map(Landmark::new).collect(toImmutableList());
        });
  }

  @Produces
  static ListLandmarksResponse createResponse(
      List<List<Landmark>> oldLandmarks, List<Landmark> newLandmarks) {
    ListLandmarksResponse.Builder response = ListLandmarksResponse.newBuilder();

    response.addAllLandmark(
        Stream.concat(oldLandmarks.stream().flatMap(List::stream), newLandmarks.stream())
                .map(ListLandmarksGraph::convertLandmark)
            ::iterator);

    return response.build();
  }

  private static org.curioswitch.cafemap.api.Landmark convertLandmark(Landmark landmark) {
    return org.curioswitch.cafemap.api.Landmark.newBuilder()
        .setId(landmark.getId().toString())
        .setGooglePlaceId(landmark.getGooglePlaceId())
        .setType(Type.PARK)
        .build();
  }

  private ListLandmarksGraph() {}
}

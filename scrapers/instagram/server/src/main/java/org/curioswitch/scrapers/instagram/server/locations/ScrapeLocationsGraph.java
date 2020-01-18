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

package org.curioswitch.scrapers.instagram.server.locations;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.spotify.futures.CompletableFuturesExtra.toListenableFuture;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.ServiceRequestContext;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import dagger.producers.ProductionSubcomponent;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Qualifier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.curioswitch.common.server.framework.grpc.GrpcProductionComponent;
import org.curioswitch.scrapers.instagram.api.ScrapeLocationsRequest;
import org.curioswitch.scrapers.instagram.api.ScrapeLocationsResponse;
import org.curioswitch.scrapers.instagram.server.models.Location;
import org.curioswitch.scrapers.instagram.server.models.LocationsPage;
import org.curioswitch.scrapers.instagram.server.models.PostPage;
import org.curioswitch.scrapers.instagram.server.models.ProfilePage;
import org.curioswitch.scrapers.instagram.server.models.TagPage;
import org.curioswitch.scrapers.instagram.server.util.SharedDataExtractor;

@ProducerModule
public class ScrapeLocationsGraph {

  @ProductionSubcomponent(modules = ScrapeLocationsGraph.class)
  public interface Component extends GrpcProductionComponent<ScrapeLocationsResponse> {

    @ProductionSubcomponent.Builder
    interface Builder
        extends GrpcProductionComponentBuilder<ScrapeLocationsGraph, Component, Builder> {}
  }

  private final ScrapeLocationsRequest request;

  public ScrapeLocationsGraph(ScrapeLocationsRequest request) {
    this.request = request;
  }

  @Qualifier
  @interface HashtagPage {}

  @Qualifier
  @interface FetchedPostPage {}

  @Qualifier
  @interface UserPage {}

  @Qualifier
  @interface LocationPage {}

  @Produces
  ScrapeLocationsRequest request() {
    return request;
  }

  @Produces
  @HashtagPage
  static ListenableFuture<List<@Nullable AggregatedHttpResponse>> fetchHashtags(
      ScrapeLocationsRequest request, WebClient instagramClient, ServiceRequestContext ctx) {
    return Futures.successfulAsList(
        request.getHashtagList().stream()
            .map(
                hashtag ->
                    toListenableFuture(
                        instagramClient
                            .get("/explore/tags/" + hashtag + '/')
                            .aggregateWithPooledObjects(ctx.eventLoop(), ctx.alloc())))
            .collect(toImmutableList()));
  }

  @Produces
  @FetchedPostPage
  static ListenableFuture<List<@Nullable AggregatedHttpResponse>> fetchPosts(
      @HashtagPage List<@Nullable AggregatedHttpResponse> hashtagPages,
      SharedDataExtractor sharedDataExtractor,
      WebClient instagramClient,
      ServiceRequestContext ctx) {
    return Futures.successfulAsList(
        hashtagPages.stream()
            .filter(Objects::nonNull)
            .map(page -> sharedDataExtractor.extractSharedData(page, TagPage.class))
            .flatMap(
                page ->
                    page.getEntryData().getTagPage().get(0).getGraphql().getHashtag().getPosts()
                        .getEdges().stream())
            .map(
                post ->
                    toListenableFuture(
                        instagramClient
                            .get("/p/" + post.getNode().getShortcode() + '/')
                            .aggregateWithPooledObjects(ctx.eventLoop(), ctx.alloc())))
            .collect(toImmutableList()));
  }

  @Produces
  @UserPage
  static ListenableFuture<List<@Nullable AggregatedHttpResponse>> fetchUserPages(
      ScrapeLocationsRequest request, WebClient instagramClient, ServiceRequestContext ctx) {
    return Futures.successfulAsList(
        request.getUsernameList().stream()
            .map(
                username ->
                    toListenableFuture(
                        instagramClient
                            .get('/' + username + '/')
                            .aggregateWithPooledObjects(ctx.eventLoop(), ctx.alloc())))
            .collect(toImmutableList()));
  }

  @Produces
  @LocationPage
  static ListenableFuture<List<@Nullable AggregatedHttpResponse>> fetchLocations(
      @FetchedPostPage List<@Nullable AggregatedHttpResponse> postPages,
      @UserPage List<@Nullable AggregatedHttpResponse> userPages,
      WebClient instagramClient,
      SharedDataExtractor sharedDataExtractor,
      ServiceRequestContext ctx) {
    return Futures.successfulAsList(
        Stream.concat(
                userPages.stream()
                    .filter(Objects::nonNull)
                    .map(page -> sharedDataExtractor.extractSharedData(page, ProfilePage.class))
                    .flatMap(ScrapeLocationsGraph::getLocationPageIds),
                postPages.stream()
                    .filter(Objects::nonNull)
                    .map(page -> sharedDataExtractor.extractSharedData(page, PostPage.class))
                    .map(ScrapeLocationsGraph::getLocationPageId)
                    .filter(s -> !s.isEmpty()))
            .distinct()
            .map(
                locationId ->
                    toListenableFuture(
                        instagramClient
                            .get("/explore/locations/" + locationId + '/')
                            .aggregateWithPooledObjects(ctx.eventLoop(), ctx.alloc())))
            .collect(toImmutableList()));
  }

  @Produces
  static ScrapeLocationsResponse buildResponse(
      @LocationPage List<@Nullable AggregatedHttpResponse> locationPages,
      SharedDataExtractor sharedDataExtractor) {
    return ScrapeLocationsResponse.newBuilder()
        .addAllLocation(
            locationPages.stream()
                    .filter(response -> response != null && response.status().equals(HttpStatus.OK))
                    .map(page -> sharedDataExtractor.extractSharedData(page, LocationsPage.class))
                    .map(ScrapeLocationsGraph::convertLocationPage)
                ::iterator)
        .build();
  }

  private static Stream<String> getLocationPageIds(ProfilePage profilePage) {
    return profilePage.getEntryData().getProfilePage().get(0).getGraphql().getUser().getTimeline()
        .getEdges().stream()
        .map(edge -> edge.getNode().getLocation())
        .filter(Objects::nonNull)
        .map(Location::getId);
  }

  private static String getLocationPageId(PostPage page) {
    var location = page.getEntryData().getPostPage().get(0).getGraphql().getPost().getLocation();
    if (location != null) {
      return location.getId();
    } else {
      return "";
    }
  }

  private static ScrapeLocationsResponse.LocationPage convertLocationPage(
      LocationsPage locationPage) {
    var location = locationPage.getEntryData().getLocationsPage().get(0).getGraphql().getLocation();
    return ScrapeLocationsResponse.LocationPage.newBuilder()
        .setId(location.getId())
        .setName(location.getName())
        .setLatitude(location.getLatitude())
        .setLongitude(location.getLongitude())
        .build();
  }
}

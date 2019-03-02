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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.curioswitch.database.cafemapdb.tables.Place.PLACE;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.server.Server;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.grpc.BindableService;
import java.util.function.Function;
import javax.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.curioswitch.common.server.framework.ServerModule;
import org.curioswitch.common.server.framework.armeria.ClientBuilderFactory;
import org.curioswitch.common.server.framework.database.DatabaseModule;
import org.curioswitch.database.cafemapdb.tables.records.PlaceRecord;
import org.curioswitch.scrapers.instagram.api.InstagramScraperServiceGrpc.InstagramScraperServiceBlockingStub;
import org.curioswitch.scrapers.instagram.api.ScrapeLocationsRequest;
import org.curioswitch.scrapers.instagram.api.ScrapeLocationsResponse.LocationPage;
import org.curioswitch.scrapers.instagram.server.locations.ScrapeLocationsGraph;
import org.jooq.DSLContext;

public class InstagramScraperServiceMain {

  private static final Logger logger = LogManager.getLogger();

  @Module(
      includes = {DatabaseModule.class, ServerModule.class},
      subcomponents = ScrapeLocationsGraph.Component.class)
  abstract static class InstagramScraperServiceModule {
    @Binds
    @IntoSet
    abstract BindableService service(InstagramScraperService service);

    @Provides
    static HttpClient instagramClient(ClientBuilderFactory factory) {
      return factory
          .create("instagram-client", "none+https://www.instagram.com/")
          .setHttpHeader(HttpHeaderNames.USER_AGENT, "CurioBot 0.1")
          .build(HttpClient.class);
    }

    @Provides
    static InstagramScraperServiceBlockingStub instagramScraperClient(
        ClientBuilderFactory factory) {
      return factory
          .create("scraper-client", "gproto+https://localhost:8080/api/")
          .build(InstagramScraperServiceBlockingStub.class);
    }

    @Provides
    static ObjectMapper objectMapper() {
      return new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(Feature.IGNORE_UNKNOWN)
          .findAndRegisterModules();
    }
  }

  @Singleton
  @Component(modules = InstagramScraperServiceModule.class)
  interface ServerComponent {
    Server server();

    DSLContext db();

    InstagramScraperServiceBlockingStub scraperClient();
  }

  public static void main(String[] args) {
    var component = DaggerInstagramScraperServiceMain_ServerComponent.create();

    component.server();
    InstagramScraperServiceBlockingStub scraperClient = component.scraperClient();

    var response =
        scraperClient.scrapeLocations(
            ScrapeLocationsRequest.newBuilder()
                .addUsername("rinkoroom")
                .addUsername("607keih")
                .addUsername("hidekazu.cafe")
                .addUsername("chiemi51s")
                .addUsername("genkihiro08061028")
                .addUsername("nicefotoco")
                .addUsername("cafemiru.jp")
                .build());

    var deduped =
        response.getLocationList().stream()
            .collect(toImmutableMap(LocationPage::getId, Function.identity(), (a, b) -> a));

    var db = component.db();

    for (var location : deduped.values()) {
      PlaceRecord record = db.newRecord(PLACE);
      record
          .setName(location.getName())
          .setInstagramId(location.getId())
          .setLatitude(location.getLatitude())
          .setLongitude(location.getLongitude());
      db.insertInto(PLACE).set(record).onDuplicateKeyUpdate().set(record).execute();
    }

    logger.info("Finished writing places.");
  }

  private InstagramScraperServiceMain() {}
}

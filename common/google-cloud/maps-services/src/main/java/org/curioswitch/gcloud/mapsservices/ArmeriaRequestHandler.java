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

package org.curioswitch.gcloud.mapsservices;

import com.google.common.collect.ImmutableSet;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeoApiContext.RequestHandler;
import com.google.maps.GeolocationApi;
import com.google.maps.PendingResult;
import com.google.maps.internal.ApiResponse;
import com.google.maps.internal.DayOfWeekAdapter;
import com.google.maps.internal.DistanceAdapter;
import com.google.maps.internal.DurationAdapter;
import com.google.maps.internal.ExceptionsAllowedToRetry;
import com.google.maps.internal.FareAdapter;
import com.google.maps.internal.GeolocationResponseAdapter;
import com.google.maps.internal.InstantAdapter;
import com.google.maps.internal.LatLngAdapter;
import com.google.maps.internal.LocalTimeAdapter;
import com.google.maps.internal.PriceLevelAdapter;
import com.google.maps.internal.SafeEnumAdapter;
import com.google.maps.internal.ZonedDateTimeAdapter;
import com.google.maps.metrics.RequestMetrics;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.AddressType;
import com.google.maps.model.Distance;
import com.google.maps.model.Duration;
import com.google.maps.model.Fare;
import com.google.maps.model.LatLng;
import com.google.maps.model.LocationType;
import com.google.maps.model.OpeningHours.Period.OpenClose.DayOfWeek;
import com.google.maps.model.PlaceDetails.Review.AspectRating.RatingType;
import com.google.maps.model.PriceLevel;
import com.google.maps.model.TravelMode;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.ClientFactoryBuilder;
import com.linecorp.armeria.client.ClientOptions;
import com.linecorp.armeria.client.ClientOptionsBuilder;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.retry.Backoff;
import com.linecorp.armeria.client.retry.RetryStrategy;
import com.linecorp.armeria.client.retry.RetryingClient;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.RequestHeaders;
import java.net.Proxy;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ArmeriaRequestHandler implements GeoApiContext.RequestHandler {

  private static final Set<HttpStatus> RETRY_STATUSES =
      ImmutableSet.of(
          HttpStatus.INTERNAL_SERVER_ERROR,
          HttpStatus.SERVICE_UNAVAILABLE,
          HttpStatus.GATEWAY_TIMEOUT);

  private static final Map<FieldNamingPolicy, Gson> GSONS = new ConcurrentHashMap<>();

  private static Gson gsonForPolicy(FieldNamingPolicy fieldNamingPolicy) {
    return GSONS.computeIfAbsent(
        fieldNamingPolicy,
        policy ->
            new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
                .registerTypeAdapter(Distance.class, new DistanceAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .registerTypeAdapter(Fare.class, new FareAdapter())
                .registerTypeAdapter(LatLng.class, new LatLngAdapter())
                .registerTypeAdapter(
                    AddressComponentType.class, new SafeEnumAdapter<>(AddressComponentType.UNKNOWN))
                .registerTypeAdapter(AddressType.class, new SafeEnumAdapter<>(AddressType.UNKNOWN))
                .registerTypeAdapter(TravelMode.class, new SafeEnumAdapter<>(TravelMode.UNKNOWN))
                .registerTypeAdapter(
                    LocationType.class, new SafeEnumAdapter<>(LocationType.UNKNOWN))
                .registerTypeAdapter(RatingType.class, new SafeEnumAdapter<>(RatingType.UNKNOWN))
                .registerTypeAdapter(DayOfWeek.class, new DayOfWeekAdapter())
                .registerTypeAdapter(PriceLevel.class, new PriceLevelAdapter())
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                .registerTypeAdapter(
                    GeolocationApi.Response.class, new GeolocationResponseAdapter())
                .setFieldNamingPolicy(policy)
                .create());
  }

  private final ClientFactory clientFactory;
  private final ClientOptions clientOptions;
  private final Map<String, WebClient> httpClients;

  ArmeriaRequestHandler(ClientFactory clientFactory, ClientOptions clientOptions) {
    this.clientFactory = clientFactory;
    this.clientOptions = clientOptions;

    httpClients = new ConcurrentHashMap<>();
  }

  @Override
  public <T, R extends ApiResponse<T>> PendingResult<T> handle(
      String hostName,
      String url,
      String userAgent,
      String experienceIdHeaderValue,
      Class<R> clazz,
      FieldNamingPolicy fieldNamingPolicy,
      long errorTimeout,
      Integer maxRetries,
      ExceptionsAllowedToRetry exceptionsAllowedToRetry,
      RequestMetrics metrics) {
    return handleMethod(
        HttpMethod.GET,
        hostName,
        url,
        HttpData.empty(),
        userAgent,
        experienceIdHeaderValue,
        clazz,
        fieldNamingPolicy,
        errorTimeout,
        maxRetries,
        exceptionsAllowedToRetry);
  }

  @Override
  public <T, R extends ApiResponse<T>> PendingResult<T> handlePost(
      String hostName,
      String url,
      String payload,
      String userAgent,
      String experienceIdHeaderValue,
      Class<R> clazz,
      FieldNamingPolicy fieldNamingPolicy,
      long errorTimeout,
      Integer maxRetries,
      ExceptionsAllowedToRetry exceptionsAllowedToRetry,
      RequestMetrics metrics) {
    return handleMethod(
        HttpMethod.POST,
        hostName,
        url,
        HttpData.ofUtf8(payload),
        userAgent,
        experienceIdHeaderValue,
        clazz,
        fieldNamingPolicy,
        errorTimeout,
        maxRetries,
        exceptionsAllowedToRetry);
  }

  private <T, R extends ApiResponse<T>> PendingResult<T> handleMethod(
      HttpMethod method,
      String hostName,
      String url,
      HttpData payload,
      String userAgent,
      String experienceIdHeaderValue,
      Class<R> clazz,
      FieldNamingPolicy fieldNamingPolicy,
      long errorTimeout,
      Integer maxRetries,
      ExceptionsAllowedToRetry exceptionsAllowedToRetry) {
    var client =
        httpClients.computeIfAbsent(
            hostName,
            host -> WebClient.builder(host).factory(clientFactory).options(clientOptions).build());

    var gson = gsonForPolicy(fieldNamingPolicy);

    var headers = RequestHeaders.builder(method, url);
    if (experienceIdHeaderValue != null) {
      headers.add("X-Goog-Maps-Experience-ID", experienceIdHeaderValue);
    }
    var request = HttpRequest.of(headers.build(), payload);

    return new ArmeriaPendingResult<>(client, request, clazz, gson);
  }

  @Override
  public void shutdown() {}

  public static class Builder implements GeoApiContext.RequestHandler.Builder {

    private final ClientFactoryBuilder clientFactoryBuilder = ClientFactory.builder();
    private final ClientOptionsBuilder clientOptionsBuilder = ClientOptions.builder();

    @Override
    public RequestHandler.Builder connectTimeout(long timeout, TimeUnit unit) {
      clientFactoryBuilder.connectTimeoutMillis(unit.toMillis(timeout));
      return this;
    }

    @Override
    public RequestHandler.Builder readTimeout(long timeout, TimeUnit unit) {
      clientOptionsBuilder.responseTimeoutMillis(unit.toMillis(timeout));
      return this;
    }

    @Override
    public RequestHandler.Builder writeTimeout(long timeout, TimeUnit unit) {
      clientOptionsBuilder.writeTimeoutMillis(unit.toMillis(timeout));
      return this;
    }

    @Override
    public RequestHandler.Builder queriesPerSecond(int maxQps) {
      // TODO(choko): Implement after https://github.com/line/armeria/issues/1701
      throw new UnsupportedOperationException(
          "ArmeriaRequestHandler does not support rate limiting yet.");
    }

    @Override
    public RequestHandler.Builder proxy(Proxy proxy) {
      throw new UnsupportedOperationException(
          "ArmeriaRequestHandler does not support proxies yet.");
    }

    @Override
    public RequestHandler.Builder proxyAuthentication(
        String proxyUserName, String proxyUserPassword) {
      throw new UnsupportedOperationException(
          "ArmeriaRequestHandler does not support proxies yet.");
    }

    @Override
    public RequestHandler build() {
      clientOptionsBuilder.decorator(
          RetryingClient.newDecorator(
              RetryStrategy.onStatus(
                  (status, t) -> {
                    if (t != null || RETRY_STATUSES.contains(status)) {
                      return Backoff.ofDefault();
                    }
                    return null;
                  })));

      return new ArmeriaRequestHandler(clientFactoryBuilder.build(), clientOptionsBuilder.build());
    }
  }
}

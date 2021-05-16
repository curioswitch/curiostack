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

package org.curioswitch.curiostack.gateway;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.server.Route;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.armeria.ClientBuilderFactory;
import org.curioswitch.curiostack.gateway.RoutingConfig.Target;

@Singleton
class RoutingConfigLoader {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

  private final ClientBuilderFactory clientBuilderFactory;

  @Inject
  RoutingConfigLoader(ClientBuilderFactory clientBuilderFactory) {
    this.clientBuilderFactory = clientBuilderFactory;
  }

  Map<Route, WebClient> load(Path configPath) {
    final RoutingConfig config;
    try {
      config = OBJECT_MAPPER.readValue(configPath.toFile(), RoutingConfig.class);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read routing config.", e);
    }

    Map<String, WebClient> clients =
        config.getTargets().stream()
            .collect(
                toImmutableMap(
                    Target::getName,
                    t ->
                        clientBuilderFactory
                            .create(t.getName(), addSerializationFormat(t.getUrl()))
                            .build(WebClient.class)));

    return config.getRules().stream()
        .collect(
            toImmutableMap(
                r -> Route.builder().path(r.getPathPattern()).build(),
                r -> clients.get(r.getTarget())));
  }

  private static String addSerializationFormat(String url) {
    return !url.contains("+") ? "none+" + url : url;
  }
}

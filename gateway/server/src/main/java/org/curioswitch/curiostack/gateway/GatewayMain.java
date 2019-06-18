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

import com.linecorp.armeria.server.Route;
import com.linecorp.armeria.server.Server;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.ServerModule;
import org.curioswitch.common.server.framework.files.FileWatcher;
import org.curioswitch.common.server.framework.server.HttpServiceDefinition;

public class GatewayMain {

  @Module(includes = ServerModule.class)
  abstract static class GatewayModule {

    @Provides
    @Singleton
    static GatewayConfig gatewayConfig(Config config) {
      return ((ModifiableGatewayConfig)
              ConfigBeanFactory.create(config.getConfig("gateway"), ModifiableGatewayConfig.class))
          .toImmutable();
    }

    @Provides
    @Singleton
    static RoutingService registerFileWatcher(
        FileWatcher.Builder fileWatcherBuilder, RoutingConfigLoader loader, GatewayConfig config) {
      Path configPath = Paths.get(config.getConfigPath());
      RoutingService service = new RoutingService(loader.load(configPath));
      fileWatcherBuilder.registerPath(configPath, path -> service.updateClients(loader.load(path)));
      return service;
    }

    @Provides
    @IntoSet
    static HttpServiceDefinition routingService(RoutingService routingService) {
      return new HttpServiceDefinition.Builder()
          .route(Route.builder().catchAll().build())
          .service(routingService)
          .build();
    }

    private GatewayModule() {}
  }

  @Singleton
  @Component(modules = GatewayModule.class)
  interface ServerComponent {
    Server server();
  }

  public static void main(String[] args) {
    DaggerGatewayMain_ServerComponent.create().server();
  }

  private GatewayMain() {}
}

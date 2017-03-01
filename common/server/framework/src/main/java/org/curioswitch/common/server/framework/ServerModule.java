/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

package org.curioswitch.common.server.framework;

import com.linecorp.armeria.common.http.HttpSessionProtocols;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder;
import dagger.Module;
import dagger.Provides;
import io.grpc.BindableService;
import java.util.Set;

/**
 * A {@link Module} which bootstraps a server, finding and registering GRPC services to expose.
 * All servers should include this {@link Module} from an application-specific {@link Module} that
 * binds services to be exposed to {@link BindableService} and add it to a {@link dagger.Component}
 * which returns the initialized {@link Server}.
 *
 * <p>For example,
 * <pre>
 *   {@code
 *   @Module(includes = ServerModule.class)
 *   abstract class MyAppServerModule {
 *     @Bind @IntoSet abstract BindableService myAppService(AppService service);
 *   }
 *
 *   @Component(modules = MyAppServerModule.class)
 *   interface MyAppComponent {
 *     Server server();
 *   }
 * </pre>
 */
@Module
public class ServerModule {

  @Provides
  Server armeriaServer(Set<BindableService> grpcServices) {
    ServerBuilder sb = new ServerBuilder().port(8080, HttpSessionProtocols.HTTP);

    GrpcServiceBuilder serviceBuilder = new GrpcServiceBuilder();
    grpcServices.forEach(serviceBuilder::addService);
    sb.serviceUnder("/", serviceBuilder.build());

    return sb.build();
  }
}

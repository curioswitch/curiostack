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

package org.curioswitch.eggworld.server;

import com.linecorp.armeria.server.Server;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.multibindings.IntoSet;
import io.grpc.BindableService;
import javax.inject.Singleton;
import org.curioswitch.common.server.framework.ServerModule;
import org.curioswitch.eggworld.server.graphs.CheckIngredientsGraph;
import org.curioswitch.eggworld.server.yummly.YummlyApiModule;

public class Main {

  @Module(
    includes = {ServerModule.class, YummlyApiModule.class},
    subcomponents = {CheckIngredientsGraph.Component.class}
  )
  abstract static class MainModule {
    @Binds
    @IntoSet
    abstract BindableService service(EggworldService service);
  }

  @Singleton
  @Component(modules = MainModule.class)
  interface MainComponent {
    Server server();
  }

  public static void main(String[] args) {
    DaggerMain_MainComponent.create().server();
  }
}

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

package org.curioswitch.cafemap.server;

import dagger.Component;
import dagger.Module;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.inject.Singleton;
import org.curioswitch.cafemap.server.places.PlaceDumper;
import org.curioswitch.common.server.framework.database.DatabaseModule;

public class CafeMapDumperMain {

  @Module(includes = {DatabaseModule.class})
  abstract static class CafeMapDumperModule {}

  @Singleton
  @Component(modules = CafeMapDumperModule.class)
  interface DumperComponent {
    PlaceDumper dumper();
  }

  public static void main(String[] args) throws Exception {
    var response = DaggerCafeMapDumperMain_DumperComponent.create().dumper().dumpAllPlaces();

    Files.write(
        Paths.get("cafe-map/client/unity/Assets/Resources/Secrets/placedb.binarypb"),
        response.toByteArray());
  }

  private CafeMapDumperMain() {}
}

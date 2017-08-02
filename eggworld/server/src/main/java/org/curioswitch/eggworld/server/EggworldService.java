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

import com.spotify.futures.FuturesExtra;
import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.curioswitch.eggworld.api.CheckIngredientsRequest;
import org.curioswitch.eggworld.api.CheckIngredientsResponse;
import org.curioswitch.eggworld.api.EggworldServiceGrpc.EggworldServiceImplBase;
import org.curioswitch.eggworld.api.FindRecipeRequest;
import org.curioswitch.eggworld.api.FindRecipeResponse;
import org.curioswitch.eggworld.server.graphs.CheckIngredientsGraph;

@Singleton
class EggworldService extends EggworldServiceImplBase {

  private final Provider<CheckIngredientsGraph.Component.Builder> checkIngredientsGraph;

  @Inject
  EggworldService(Provider<CheckIngredientsGraph.Component.Builder> checkIngredientsGraph) {
    this.checkIngredientsGraph = checkIngredientsGraph;
  }

  @Override
  public void checkIngredients(CheckIngredientsRequest request,
      StreamObserver<CheckIngredientsResponse> responseObserver) {
    FuturesExtra.addCallback(
        checkIngredientsGraph.get().graph(new CheckIngredientsGraph(request)).build().execute(),
        response -> {
          responseObserver.onNext(response);
          responseObserver.onCompleted();
        },
        responseObserver::onError);
  }

  @Override
  public void findRecipe(FindRecipeRequest request,
      StreamObserver<FindRecipeResponse> responseObserver) {
    super.findRecipe(request, responseObserver);
  }
}

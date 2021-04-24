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

package org.curioswitch.eggworld.server.graphs;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.linecorp.armeria.common.RequestContext;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import dagger.producers.ProductionSubcomponent;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.curioswitch.common.server.framework.grpc.GrpcProductionComponent;
import org.curioswitch.eggworld.api.FindRecipeRequest;
import org.curioswitch.eggworld.api.FindRecipeResponse;
import org.curioswitch.eggworld.server.EggworldConstants;
import org.curioswitch.eggworld.server.util.IngredientConverter;
import org.curioswitch.eggworld.server.yummly.YummlyApi;
import org.curioswitch.eggworld.server.yummly.models.Recipe;
import org.curioswitch.eggworld.server.yummly.models.SearchResponse;

@ProducerModule
public class FindRecipeGraph {

  @ProductionSubcomponent(modules = {FindRecipeGraph.class})
  public interface Component extends GrpcProductionComponent<FindRecipeResponse> {

    @ProductionSubcomponent.Builder
    interface Builder extends GrpcProductionComponentBuilder<FindRecipeGraph, Component, Builder> {}
  }

  private final FindRecipeRequest request;

  public FindRecipeGraph(FindRecipeRequest request) {
    this.request = request;
  }

  @Produces
  FindRecipeRequest request() {
    return request;
  }

  @Produces
  static List<String> ingredients(FindRecipeRequest request) {
    return ImmutableList.<String>builder()
        .addAll(IngredientConverter.FORWARD.convertAll(request.getIngredientList()))
        .add(EggworldConstants.EGGS_INGREDIENT)
        .build();
  }

  @Produces
  static ListenableFuture<SearchResponse> doSearch(List<String> ingredients, YummlyApi yummly) {
    return yummly.search(EggworldConstants.EGG_QUERY, ingredients, 0, 1, true, ImmutableList.of());
  }

  @Produces
  static ListenableFuture<Recipe> recipe(
      List<String> ingredients,
      SearchResponse searchResponse,
      Supplier<Random> randomSupplier,
      YummlyApi yummly) {
    int totalCount = searchResponse.totalMatchCount();

    ListenableFuture<SearchResponse> future = Futures.immediateFuture(null);
    // Get a random recipe to return. Search request fails randomly so try a few times.
    Executor executor = RequestContext.current().eventLoop();
    Random random = randomSupplier.get();
    for (int i = 0; i < 5; i++) {
      int resultIndex = random.nextInt(totalCount);
      future =
          Futures.transformAsync(
              future,
              result -> {
                if (result != null && !result.matches().isEmpty()) {
                  return Futures.immediateFuture(result);
                }
                return yummly.search(
                    EggworldConstants.EGG_QUERY,
                    ingredients,
                    resultIndex,
                    1,
                    true,
                    ImmutableList.of());
              },
              executor);
    }

    return Futures.transform(future, r -> r.matches().get(0), MoreExecutors.directExecutor());
  }

  @Produces
  static FindRecipeResponse response(Recipe recipe) {
    return FindRecipeResponse.newBuilder()
        .setRecipeUrl("http://www.yummly.com/recipe/" + recipe.id())
        .build();
  }
}

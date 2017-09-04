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

package org.curioswitch.eggworld.server.graphs;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import dagger.producers.ProductionSubcomponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.curioswitch.eggworld.api.CheckIngredientsRequest;
import org.curioswitch.eggworld.api.CheckIngredientsResponse;
import org.curioswitch.eggworld.api.Ingredient;
import org.curioswitch.eggworld.server.EggworldConstants;
import org.curioswitch.eggworld.server.util.IngredientConverter;
import org.curioswitch.eggworld.server.yummly.YummlyApi;
import org.curioswitch.eggworld.server.yummly.models.SearchResponse;

@ProducerModule
public class CheckIngredientsGraph {

  private static final List<String> INGREDIENT_FACET = ImmutableList.of("ingredient");

  private static final Set<String> SUPPORTED_INGREDIENTS =
      Arrays.stream(Ingredient.values())
          .filter(i -> i != Ingredient.UNRECOGNIZED)
          .map(IngredientConverter.FORWARD::convert)
          .collect(toImmutableSet());

  @ProductionSubcomponent(modules = {CheckIngredientsGraph.class})
  public interface Component {

    ListenableFuture<CheckIngredientsResponse> execute();

    @ProductionSubcomponent.Builder
    interface Builder {
      Builder graph(CheckIngredientsGraph graph);

      Component build();
    }
  }

  private final CheckIngredientsRequest request;

  public CheckIngredientsGraph(CheckIngredientsRequest request) {
    this.request = request;
  }

  @Produces
  CheckIngredientsRequest request() {
    return request;
  }

  @Produces
  ListenableFuture<SearchResponse> doSearch(CheckIngredientsRequest request, YummlyApi yummly) {
    List<String> ingredients =
        ImmutableList.<String>builder()
            .addAll(IngredientConverter.FORWARD.convertAll(request.getSelectedIngredientList()))
            .add(EggworldConstants.EGGS_INGREDIENT)
            .build();
    return yummly.search(EggworldConstants.EGG_QUERY, ingredients, 1, true, INGREDIENT_FACET);
  }

  @Produces
  CheckIngredientsResponse response(SearchResponse searchResponse) {
    List<Ingredient> availableIngredients =
        searchResponse
            .facetCounts()
            .ingredient()
            .entrySet()
            .stream()
            .filter(e -> SUPPORTED_INGREDIENTS.contains(e.getKey()) && e.getValue() > 0)
            .map(e -> IngredientConverter.REVERSE.convert(e.getKey()))
            .collect(toImmutableList());
    return CheckIngredientsResponse.newBuilder()
        .addAllSelectableIngredient(availableIngredients)
        .build();
  }
}

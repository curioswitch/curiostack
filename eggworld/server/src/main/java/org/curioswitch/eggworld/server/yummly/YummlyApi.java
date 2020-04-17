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
package org.curioswitch.eggworld.server.yummly;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.curioswitch.eggworld.server.yummly.models.SearchResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** The Yummly API, for finding recipes. */
public interface YummlyApi {

  @GET("recipes")
  ListenableFuture<SearchResponse> search(
      @Query("q") String query,
      @Query("allowedIngredient[]") List<String> ingredients,
      @Query("start") int start,
      @Query("maxResult") int maxResult,
      @Query("requirePictures") boolean requirePictures,
      @Query("facetField[]") List<String> facetFields);
}

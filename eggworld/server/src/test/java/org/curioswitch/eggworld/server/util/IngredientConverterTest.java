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
package org.curioswitch.eggworld.server.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import org.curioswitch.eggworld.api.Ingredient;
import org.junit.jupiter.api.Test;

class IngredientConverterTest {

  @Test
  void toYummly() {
    for (var ingredient : Ingredient.values()) {
      if (ingredient == Ingredient.UNRECOGNIZED) {
        continue;
      }
      assertThat(IngredientConverter.FORWARD.convert(ingredient))
          .isEqualTo(ingredient.name().toLowerCase());
    }
  }

  @Test
  void fromYummly() {
    for (var ingredient : Ingredient.values()) {
      if (ingredient == Ingredient.UNRECOGNIZED) {
        continue;
      }
      assertThat(IngredientConverter.REVERSE.convert(ingredient.name().toLowerCase()))
          .isEqualTo(ingredient);
    }
  }

  @Test
  void unrecognized() {
    assertThatThrownBy(() -> IngredientConverter.FORWARD.convert(Ingredient.UNRECOGNIZED))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> IngredientConverter.REVERSE.convert("foobarchoko"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

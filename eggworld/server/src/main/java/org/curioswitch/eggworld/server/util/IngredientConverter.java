/**
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

import com.google.common.base.Converter;
import org.curioswitch.eggworld.api.Ingredient;

/** Converter of {@link Ingredient} for use with Yummly API. */
public final class IngredientConverter extends Converter<Ingredient, String> {

  public static final Converter<Ingredient, String> FORWARD = new IngredientConverter();
  public static final Converter<String, Ingredient> REVERSE = FORWARD.reverse();

  @Override
  protected String doForward(Ingredient ingredient) {
    switch (ingredient) {
      case BANANA:
        return "banana";
      case CABBAGE:
        return "cabbage";
      case GARLIC:
        return "garlic";
      case ONION:
        return "onion";
      case POTATOES:
        return "potatoes";
      case SPINACH:
        return "spinach";
      case BACON:
        return "bacon";
      case BEEF:
        return "beef";
      case CHEESE:
        return "cheese";
      case CHICKEN:
        return "chicken";
      case MILK:
        return "milk";
      case PORK:
        return "pork";
      case BREAD:
        return "bread";
      case CHILE:
        return "chile";
      case CHOCOLATE:
        return "chocolate";
      case HONEY:
        return "honey";
      case NUTS:
        return "nuts";
      case WINE:
        return "wine";
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException("Unrecognized ingredient: " + ingredient);
    }
  }

  @Override
  protected Ingredient doBackward(String yummlyIngredient) {
    switch (yummlyIngredient) {
      case "banana":
        return Ingredient.BANANA;
      case "cabbage":
        return Ingredient.CABBAGE;
      case "garlic":
        return Ingredient.GARLIC;
      case "onion":
        return Ingredient.ONION;
      case "potatoes":
        return Ingredient.POTATOES;
      case "spinach":
        return Ingredient.SPINACH;
      case "bacon":
        return Ingredient.BACON;
      case "beef":
        return Ingredient.BEEF;
      case "cheese":
        return Ingredient.CHEESE;
      case "chicken":
        return Ingredient.CHICKEN;
      case "milk":
        return Ingredient.MILK;
      case "pork":
        return Ingredient.PORK;
      case "bread":
        return Ingredient.BREAD;
      case "chile":
        return Ingredient.CHILE;
      case "chocolate":
        return Ingredient.CHOCOLATE;
      case "honey":
        return Ingredient.HONEY;
      case "nuts":
        return Ingredient.NUTS;
      case "wine":
        return Ingredient.WINE;
      default:
        throw new IllegalArgumentException("Unrecognized ingredient: " + yummlyIngredient);
    }
  }

  private IngredientConverter() {}
}

/*
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

import { Ingredient } from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import baconImageSrc from './assets/bacon.png';
import bananaImageSrc from './assets/banana.png';
import beefImageSrc from './assets/beef.png';
import breadImageSrc from './assets/bread.png';
import cabbageImageSrc from './assets/cabbage.png';
import cheeseImageSrc from './assets/cheese.png';
import chickenImageSrc from './assets/chicken.png';
import chileImageSrc from './assets/chile.png';
import chocolateImageSrc from './assets/chocolate.png';
import garlicImageSrc from './assets/garlic.png';
import honeyImageSrc from './assets/honey.png';
import milkImageSrc from './assets/milk.png';
import nutsImageSrc from './assets/nuts.png';
import onionImageSrc from './assets/onion.png';
import porkImageSrc from './assets/pork.png';
import potatoesImageSrc from './assets/potatoes.png';
import spinachImageSrc from './assets/spinach.png';
import wineImageSrc from './assets/wine.png';

export interface IngredientDescriptor {
  key: Ingredient;
  name: string;
  imageSrc: string;
}

interface Ingredients {
  fruit: IngredientDescriptor[];
  meat: IngredientDescriptor[];
  other: IngredientDescriptor[];
}

export const INGREDIENTS: Ingredients = {
  fruit: [
    { key: Ingredient.BANANA, name: 'バナナ', imageSrc: bananaImageSrc },
    { key: Ingredient.CABBAGE, name: 'キャベツ', imageSrc: cabbageImageSrc },
    { key: Ingredient.GARLIC, name: 'にんにく', imageSrc: garlicImageSrc },
    { key: Ingredient.ONION, name: '玉ねぎ', imageSrc: onionImageSrc },
    {
      key: Ingredient.POTATOES,
      name: 'じゃがいも',
      imageSrc: potatoesImageSrc,
    },
    { key: Ingredient.SPINACH, name: 'ほうれん草', imageSrc: spinachImageSrc },
  ],
  meat: [
    { key: Ingredient.BACON, name: 'ベーコン', imageSrc: baconImageSrc },
    { key: Ingredient.BEEF, name: '牛肉', imageSrc: beefImageSrc },
    { key: Ingredient.CHEESE, name: 'チーズ', imageSrc: cheeseImageSrc },
    { key: Ingredient.CHICKEN, name: '鶏肉', imageSrc: chickenImageSrc },
    { key: Ingredient.MILK, name: '牛乳', imageSrc: milkImageSrc },
    { key: Ingredient.PORK, name: '豚肉', imageSrc: porkImageSrc },
  ],
  other: [
    { key: Ingredient.BREAD, name: 'パン', imageSrc: breadImageSrc },
    { key: Ingredient.CHILE, name: '唐辛子', imageSrc: chileImageSrc },
    {
      key: Ingredient.CHOCOLATE,
      name: 'チョコレート',
      imageSrc: chocolateImageSrc,
    },
    { key: Ingredient.HONEY, name: 'はちみつ', imageSrc: honeyImageSrc },
    { key: Ingredient.NUTS, name: 'ナツ', imageSrc: nutsImageSrc },
    { key: Ingredient.WINE, name: 'ワイン', imageSrc: wineImageSrc },
  ],
};

interface IngredientsMap {
  [key: number]: IngredientDescriptor;
}

export const INGREDIENTS_MAP: IngredientsMap = [
  ...INGREDIENTS.fruit,
  ...INGREDIENTS.meat,
  ...INGREDIENTS.other,
].reduce(
  (map, ingredientDetails) => ({
    ...map,
    [ingredientDetails.key]: ingredientDetails,
  }),
  {},
);

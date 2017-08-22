/*
 *
 * HomePage constants
 *
 */

// @flow

import { Ingredient } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';

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
import onionImageSrc from './assets/onion.png';
import porkImageSrc from './assets/pork.png';
import potatoesImageSrc from './assets/potatoes.png';
import spinachImageSrc from './assets/spinach.png';
import nutsImageSrc from './assets/nuts.png';
import wineImageSrc from './assets/wine.png';

export const INGREDIENTS = {
  fruit: [
    { key: Ingredient.BANANA, name: 'バナナ', imageSrc: bananaImageSrc },
    { key: Ingredient.CABBAGE, name: 'キャベツ', imageSrc: cabbageImageSrc },
    { key: Ingredient.GARLIC, name: 'にんにく', imageSrc: garlicImageSrc },
    { key: Ingredient.ONION, name: '玉ねぎ', imageSrc: onionImageSrc },
    { key: Ingredient.POTATO, name: 'じゃがいも', imageSrc: potatoesImageSrc },
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
    { key: Ingredient.CHILI, name: '唐辛子', imageSrc: chileImageSrc },
    { key: Ingredient.CHOCOLATE, name: 'チョコレート', imageSrc: chocolateImageSrc },
    { key: Ingredient.HONEY, name: 'はちみつ', imageSrc: honeyImageSrc },
    { key: Ingredient.NUTS, name: 'ナツ', imageSrc: nutsImageSrc },
    { key: Ingredient.WINE, name: 'ワイン', imageSrc: wineImageSrc },
  ],
};

/*
 *
 * HomePage constants
 *
 */

// @flow

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

export const DEFAULT_ACTION = 'app/HomePage/DEFAULT_ACTION';

export const INGREDIENTS = {
  fruit: [
    { key: 'banana', name: 'バナナ', imageSrc: bananaImageSrc },
    { key: 'cabbage', name: 'キャベツ', imageSrc: cabbageImageSrc },
    { key: 'garlic', name: 'にんにく', imageSrc: garlicImageSrc },
    { key: 'onion', name: '玉ねぎ', imageSrc: onionImageSrc },
    { key: 'potatoes', name: 'じゃがいも', imageSrc: potatoesImageSrc },
    { key: 'spinach', name: 'ほうれん草', imageSrc: spinachImageSrc },
  ],
  meat: [
    { key: 'bacon', name: 'ベーコン', imageSrc: baconImageSrc },
    { key: 'beef', name: '牛肉', imageSrc: beefImageSrc },
    { key: 'cheese', name: 'チーズ', imageSrc: cheeseImageSrc },
    { key: 'chicken', name: '鶏肉', imageSrc: chickenImageSrc },
    { key: 'milk', name: '牛乳', imageSrc: milkImageSrc },
    { key: 'pork', name: '豚肉', imageSrc: porkImageSrc },
  ],
  other: [
    { key: 'bread', name: 'パン', imageSrc: breadImageSrc },
    { key: 'chile', name: '唐辛子', imageSrc: chileImageSrc },
    { key: 'chocolate', name: 'チョコレート', imageSrc: chocolateImageSrc },
    { key: 'honey', name: 'はちみつ', imageSrc: honeyImageSrc },
    { key: 'nuts', name: 'ナツ', imageSrc: nutsImageSrc },
    { key: 'wine', name: 'ワイン', imageSrc: wineImageSrc },
  ],
};

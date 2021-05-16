/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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

import { ActionsUnion, createAction } from '@curiostack/base-web';
import Konva from 'konva';
import { bindActionCreators, Dispatch } from 'redux';

import {
  CheckIngredientsResponse,
  Ingredient,
} from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

export enum ActionTypes {
  CHECK_INGREDIENTS = 'HomePage/CHECK_INGREDIENTS',
  CHECK_INGREDIENTS_RESPONSE = 'HomePage/CHECK_INGREDIENTS_RESPONSE',
  COOK = 'HomePage/COOK',
  COOK_RESPONSE = 'HomePage/COOK_RESPONSE',
  DRAW_STAGE = 'HomePage/DRAW_STAGE',
  EGG_BREAKING_DONE = 'HomePage/EGG_BREAKING_DONE',
  FOOD_DRAGGED = 'HomePage/FOOD_DRAGGED',
  MOUTH_ANIMATION_FRAME = 'HomePage/MOUTH_ANIMATION_FRAME',
  ROTATE_HAMMER = 'HomePage/ROTATE_HAMMER',
  SELECT_TAB = 'HomePage/SELECT_TAB',
}

// eslint-disable-next-line import/export
export const Actions = {
  checkIngredients: (ingredients: Ingredient[]) =>
    createAction(ActionTypes.CHECK_INGREDIENTS, ingredients),
  checkIngredientsResponse: (response: CheckIngredientsResponse) =>
    createAction(ActionTypes.CHECK_INGREDIENTS_RESPONSE, response),
  cook: () => createAction(ActionTypes.COOK),
  cookResponse: (recipeUrl: string) =>
    createAction(ActionTypes.COOK_RESPONSE, recipeUrl),
  drawStage: () => createAction(ActionTypes.DRAW_STAGE),
  eggBreakingDone: () => createAction(ActionTypes.EGG_BREAKING_DONE),
  foodDragged: (ingredient: Ingredient, node: Konva.Node) =>
    createAction(ActionTypes.FOOD_DRAGGED, {
      ingredient,
      node,
    }),
  mouthAnimationFrame: () => createAction(ActionTypes.MOUTH_ANIMATION_FRAME),
  rotateHammer: (delta: number) =>
    createAction(ActionTypes.ROTATE_HAMMER, delta),
  selectTab: (tab: 'fruit' | 'meat' | 'other') =>
    createAction(ActionTypes.SELECT_TAB, tab),
};

// eslint-disable-next-line import/export
export type Actions = ActionsUnion<typeof Actions>;

export type DispatchProps = typeof Actions;

export function mapDispatchToProps(dispatch: Dispatch<any>): DispatchProps {
  return bindActionCreators(Actions, dispatch);
}

export default Actions;

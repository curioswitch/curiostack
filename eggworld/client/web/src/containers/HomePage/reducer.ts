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

import { Record, Set } from 'immutable';
import { Node } from 'konva';

import { Ingredient } from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import { Actions, ActionTypes } from './actions';

interface StateProps {
  cooking: boolean;
  drawStageCount: number;
  eatenFood: Set<Ingredient>;
  eggBreakingDone: boolean;
  foodBeingEaten?: Ingredient;
  hammerRotation: number;
  recipeUrl?: string;
  selectedTab: 'fruit' | 'meat' | 'other';
  usableFood: Set<any>;
}

export type State = Readonly<StateProps> & Record<StateProps>;

export const initialState = Record<StateProps>({
  cooking: false,
  drawStageCount: 0,
  eatenFood: Set(),
  eggBreakingDone: false,
  foodBeingEaten: undefined,
  hammerRotation: 0,
  selectedTab: 'fruit',
  usableFood: Set(),
})();

const MOUTH_RECTANGLE = {
  x1: 480,
  y1: 430,
  x2: 580,
  y2: 510,
};

function isInsideMouth(node: Node) {
  const centerX = node.x() + node.width() / 2;
  const centerY = node.y() + node.height() / 2;
  return (
    centerX >= MOUTH_RECTANGLE.x1 &&
    centerX <= MOUTH_RECTANGLE.x2 &&
    centerY >= MOUTH_RECTANGLE.y1 &&
    centerY <= MOUTH_RECTANGLE.y2
  );
}

// We don't keep this in state since we don't need it to influence rendering.
let mouthAnimationFrameCount = 0;

export default function reducer(state: State, action: Actions) {
  switch (action.type) {
    case ActionTypes.CHECK_INGREDIENTS_RESPONSE:
      return state.set(
        'usableFood',
        Set(action.payload.getSelectableIngredientList()),
      );
    case ActionTypes.COOK:
      return state.set('cooking', true);
    case ActionTypes.COOK_RESPONSE:
      if (state.eggBreakingDone) {
        window.location.href = action.payload;
      }
      return state.set('recipeUrl', action.payload);
    case ActionTypes.DRAW_STAGE:
      return state.update('drawStageCount', (count) => count + 1);
    case ActionTypes.EGG_BREAKING_DONE:
      const recipeUrl = state.recipeUrl;
      if (recipeUrl) {
        window.location.href = recipeUrl;
      }
      return state.set('eggBreakingDone', true);
    case ActionTypes.FOOD_DRAGGED:
      if (
        state.foodBeingEaten === undefined &&
        isInsideMouth(action.payload.node)
      ) {
        return state.set('foodBeingEaten', action.payload.ingredient);
      }
      return state;
    case ActionTypes.MOUTH_ANIMATION_FRAME:
      let newState = state;
      if (mouthAnimationFrameCount === 12) {
        const ingredient = state.foodBeingEaten!;
        newState = state.update('eatenFood', (eatenFood) =>
          eatenFood.add(ingredient),
        );
      }
      if (mouthAnimationFrameCount === 24) {
        mouthAnimationFrameCount = 0;
        newState = state.remove('foodBeingEaten');
      } else {
        mouthAnimationFrameCount += 1;
      }
      return newState;
    case ActionTypes.ROTATE_HAMMER:
      return state.update(
        'hammerRotation',
        (rotation) => rotation + action.payload,
      );
    case ActionTypes.SELECT_TAB:
      return state.set('selectedTab', action.payload);
    default:
      return state;
  }
}

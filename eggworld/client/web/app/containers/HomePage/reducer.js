/*
 *
 * HomePage reducer
 *
 */

import type { CheckIngredientsResponse } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import { fromJS, Set } from 'immutable';
import { handleActions } from 'redux-actions';
import {
  checkIngredientsResponse,
  drawStage,
  foodDragged,
  mouthAnimationFrame,
  selectTab,
} from './actions';
import { INGREDIENTS } from './constants';

const MOUTH_RECTANGLE = {
  x1: 480,
  y1: 430,
  x2: 580,
  y2: 510,
};

function isInsideMouth(node) {
  const centerX = node.getX() + (node.getWidth() / 2);
  const centerY = node.getY() + (node.getHeight() / 2);
  return centerX >= MOUTH_RECTANGLE.x1
    && centerX <= MOUTH_RECTANGLE.x2
    && centerY >= MOUTH_RECTANGLE.y1
    && centerY <= MOUTH_RECTANGLE.y2;
}

const initialState = fromJS({
  eatenFood: Set(),
  foodBeingEaten: null,
  selectedTab: 'fruit',
  usableFood: Set(INGREDIENTS.fruit.concat(INGREDIENTS.meat).concat(INGREDIENTS.other).map((item) => item.key)),
});

// We don't keep this in state since we don't need it to influence rendering.
let mouthAnimationFrameCount = 0;

export default handleActions({
  [checkIngredientsResponse]: (state, { payload }: { payload: CheckIngredientsResponse }) =>
    state.set('usableFood', Set(payload.getSelectableIngredientList())),
  [drawStage]: (state) => state.update('drawStageCount', (count) => count + 1),
  [foodDragged]: (state, { payload }) => (isInsideMouth(payload.node) ? state.set('foodBeingEaten', payload) : state),
  [mouthAnimationFrame]: (state) => state.withMutations((mutable) => {
    if (mouthAnimationFrameCount === 12) {
      const { ingredient } = state.get('foodBeingEaten');
      mutable.update('eatenFood', (eatenFood) => eatenFood.add(ingredient));
    }
    if (mouthAnimationFrameCount === 24) {
      mouthAnimationFrameCount = 0;
      mutable.set('foodBeingEaten', null);
    } else {
      mouthAnimationFrameCount += 1;
    }
  }),
  [selectTab]: (state, { payload }) => state.set('selectedTab', payload),
}, initialState);

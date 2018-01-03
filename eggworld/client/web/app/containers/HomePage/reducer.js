/*
 *
 * HomePage reducer
 *
 */

import type { CheckIngredientsResponse } from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import { fromJS, Set } from 'immutable';
import { handleActions } from 'redux-actions';
import {
  checkIngredientsResponse,
  cook,
  cookResponse,
  drawStage,
  eggBreakingDone,
  foodDragged,
  mouthAnimationFrame,
  rotateHammer,
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
  cooking: false,
  eatenFood: Set(),
  eggBreakingDone: false,
  foodBeingEaten: null,
  hammerRotation: 0,
  recipeUrl: null,
  selectedTab: 'fruit',
  usableFood: Set(INGREDIENTS.fruit.concat(INGREDIENTS.meat).concat(INGREDIENTS.other).map((item) => item.key)),
});

// We don't keep this in state since we don't need it to influence rendering.
let mouthAnimationFrameCount = 0;

export default handleActions({
  [checkIngredientsResponse]: (state, { payload }: { payload: CheckIngredientsResponse }) =>
    state.set('usableFood', Set(payload.getSelectableIngredientList())),
  [cook]: (state) => state.set('cooking', true),
  [cookResponse]: (state, { payload }) => {
    if (state.get('eggBreakingDone')) {
      window.location.href = payload;
    }
    return state.set('recipeUrl', payload);
  },
  [drawStage]: (state) => state.update('drawStageCount', (count) => count + 1),
  [eggBreakingDone]: (state) => {
    const recipeUrl = state.get('recipeUrl');
    if (recipeUrl) {
      window.location.href = recipeUrl;
    }
    return state.set('eggBreakingDone', true);
  },
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
  [rotateHammer]: (state, { payload }) => state.update('hammerRotation', (rotation) => rotation + payload),
  [selectTab]: (state, { payload }) => state.set('selectedTab', payload),
}, initialState);

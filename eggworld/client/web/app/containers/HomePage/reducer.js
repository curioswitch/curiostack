/*
 *
 * HomePage reducer
 *
 */

import { fromJS, Set } from 'immutable';
import { handleActions } from 'redux-actions';
import {
  drawStage,
  foodDragged,
  mouthAnimationFrame,
  selectTab,
} from './actions';

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
  mouthAnimationFrameCount: 0,
  selectedTab: 'fruit',
});

export default handleActions({
  [drawStage]: (state) => state.update('drawStageCount', (count) => count + 1),
  [foodDragged]: (state, { payload }) => (isInsideMouth(payload.node) ? state.set('foodBeingEaten', payload) : state),
  [mouthAnimationFrame]: (state) => state.withMutations((mutable) => {
    const count = state.get('mouthAnimationFrameCount');
    if (count === 12) {
      const { ingredient } = state.get('foodBeingEaten');
      mutable.update('eatenFood', (eatenFood) => eatenFood.add(ingredient));
    }
    if (count === 24) {
      mutable.set('mouthAnimationFrameCount', 0).set('foodBeingEaten', null);
    } else {
      mutable.update('mouthAnimationFrameCount', (c) => c + 1);
    }
  }),
  [selectTab]: (state, { payload }) => state.set('selectedTab', payload),
}, initialState);

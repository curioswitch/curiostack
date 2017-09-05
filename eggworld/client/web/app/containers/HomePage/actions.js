/*
 *
 * HomePage actions
 *
 */

// @flow

import { createAction } from 'redux-actions';

export const checkIngredients = createAction('app/containers/HomePage/CHECK_INGREDIENTS');
export const checkIngredientsResponse = createAction('app/containers/HomePage/CHECK_INGREDIENTS_RESPONSE');
export const drawStage = createAction('app/containers/HomePage/DRAW_STAGE');
export const foodDragged = createAction('app/containers/HomePage/FOOD_DRAGGED');
export const mouthAnimationFrame = createAction('app/containers/HomePage/MOUTH_ANIMATION_FRAME');
export const rotateHammer = createAction('app/containers/HomePage/ROTATE_HAMMER');
export const selectTab = createAction('app/containers/HomePage/SELECT_TAB');

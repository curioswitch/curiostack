/**
 *
 * HomePage actions
 *
 */

import { Node } from 'konva';
import { createAction } from 'typesafe-actions';

import {
  CheckIngredientsResponse,
  Ingredient,
} from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

export const checkIngredients = createAction(
  'app/containers/HomePage/CHECK_INGREDIENTS',
);

const CHECK_INGREDIENTS_RESPONSE =
  'app/containers/HomePage/CHECK_INGREDIENTS_RESPONSE';
export const checkIngredientsResponse = createAction(
  CHECK_INGREDIENTS_RESPONSE,
  (response: CheckIngredientsResponse) => ({
    type: CHECK_INGREDIENTS_RESPONSE,
    payload: response,
  }),
);

export const cook = createAction('app/containers/HomePage/COOK');

const COOK_RESPONSE = 'app/containers/HomePage/COOK_RESPONSE';
export const cookResponse = createAction(
  COOK_RESPONSE,
  (recipeUrl: string) => ({
    type: COOK_RESPONSE,
    payload: recipeUrl,
  }),
);

export const drawStage = createAction('app/containers/HomePage/DRAW_STAGE');

export const eggBreakingDone = createAction(
  'app/containers/HomePage/EGG_BREAKING_DONE',
);

const FOOD_DRAGGED = 'app/containers/HomePage/FOOD_DRAGGED';
export const foodDragged = createAction(
  FOOD_DRAGGED,
  (ingredient: Ingredient, node: Node) => ({
    type: FOOD_DRAGGED,
    payload: {
      ingredient,
      node,
    },
  }),
);

export const mouthAnimationFrame = createAction(
  'app/containers/HomePage/MOUTH_ANIMATION_FRAME',
);

const ROTATE_HAMMER = 'app/containers/HomePage/ROTATE_HAMMER';
export const rotateHammer = createAction(ROTATE_HAMMER, (delta: number) => ({
  type: ROTATE_HAMMER,
  payload: delta,
}));

const SELECT_TAB = 'app/containers/HomePage/SELECT_TAB';
export const selectTab = createAction(
  SELECT_TAB,
  (tab: 'fruit' | 'meat' | 'other') => ({
    type: SELECT_TAB,
    payload: tab,
  }),
);

import { Node } from 'konva';
import { Dispatch } from 'react-redux';
import { bindActionCreators } from 'redux';
import { createAction } from 'typesafe-actions';
import { $call } from 'utility-types';

import {
  CheckIngredientsResponse,
  Ingredient,
} from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

const types = {
  checkIngredients: 'HomePage/CHECK_INGREDIENTS',
  checkIngredientsResponse: 'HomePage/CHECK_INGREDIENTS_RESPONSE',
  cook: 'HomePage/COOK',
  cookResponse: 'HomePage/COOK_RESPONSE',
  drawStage: 'HomePage/DRAW_STAGE',
  eggBreakingDone: 'HomePage/EGG_BREAKING_DONE',
  foodDragged: 'HomePage/FOOD_DRAGGED',
  mouthAnimationFrame: 'HomePage/MOUTH_ANIMATION_FRAME',
  rotateHammer: 'HomePage/ROTATE_HAMMER',
  selectTab: 'HomePage/SELECT_TAB',
};

const actions = {
  checkIngredients: createAction(
    types.checkIngredients,
    (ingredients: Ingredient[]) => ({
      type: types.checkIngredients,
      payload: ingredients,
    }),
  ),
  checkIngredientsResponse: createAction(
    types.checkIngredientsResponse,
    (response: CheckIngredientsResponse) => ({
      type: types.checkIngredientsResponse,
      payload: response,
    }),
  ),
  cook: createAction(types.cook),
  cookResponse: createAction(types.cookResponse, (recipeUrl: string) => ({
    type: types.cookResponse,
    payload: recipeUrl,
  })),
  drawStage: createAction(types.drawStage),
  eggBreakingDone: createAction(types.eggBreakingDone),
  foodDragged: createAction(
    types.foodDragged,
    (ingredient: Ingredient, node: Node) => ({
      type: types.foodDragged,
      payload: {
        ingredient,
        node,
      },
    }),
  ),
  mouthAnimationFrame: createAction(types.mouthAnimationFrame),
  rotateHammer: createAction(types.rotateHammer, (delta: number) => ({
    type: types.rotateHammer,
    payload: delta,
  })),
  selectTab: createAction(
    types.selectTab,
    (tab: 'fruit' | 'meat' | 'other') => ({
      type: types.selectTab,
      payload: tab,
    }),
  ),
};

export type DispatchProps = typeof actions;

export function mapDispatchToProps(dispatch: Dispatch<{}>): DispatchProps {
  return bindActionCreators(actions, dispatch);
}

const returnsOfActions = Object.values(actions).map($call);
export type Action = typeof returnsOfActions[number];

export default actions;

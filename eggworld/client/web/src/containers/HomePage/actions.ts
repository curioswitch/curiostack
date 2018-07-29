import { ActionsUnion, createAction } from '@curiostack/base-web';
import { Node } from 'konva';
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
  foodDragged: (ingredient: Ingredient, node: Node) =>
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

export type Actions = ActionsUnion<typeof Actions>;

export type DispatchProps = typeof Actions;

export function mapDispatchToProps(dispatch: Dispatch<any>): DispatchProps {
  return bindActionCreators(Actions, dispatch);
}

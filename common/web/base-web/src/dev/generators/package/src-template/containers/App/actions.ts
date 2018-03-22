import { bindActionCreators, Dispatch } from 'redux';
import { createAction } from 'typesafe-actions';
import { $call } from 'utility-types';

const types = {
  setGlobalErrorMessage: 'App/SET_GLOBAL_ERROR_MESSAGE',
};

const actions = {
  setGlobalErrorMessage: createAction(
    types.setGlobalErrorMessage,
    (errorMessage: string) => ({
      type: types.setGlobalErrorMessage,
      payload: errorMessage,
    }),
  ),
};

export type DispatchProps = typeof actions;

export function mapDispatchToProps(dispatch: Dispatch<any>): any {
  return bindActionCreators(actions, dispatch);
}

const returnsOfActions = Object.values(actions).map($call);
export type Action = typeof returnsOfActions[number];

export default actions;

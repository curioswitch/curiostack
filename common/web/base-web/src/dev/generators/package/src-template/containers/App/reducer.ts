import { Record } from 'immutable';
import { getType } from 'typesafe-actions';

import actions, { Action } from './actions';

export interface StateProps {
  globalErrorMessage: string;
}

export type State = Readonly<StateProps> & Record<StateProps>;

export const initialState = Record<StateProps>({
  globalErrorMessage: '',
})();

export default function(state: State, action: Action): State {
  switch (action.type) {
    case getType(actions.setGlobalErrorMessage):
      return state.set('globalErrorMessage', action.payload);
    default:
      return state;
  }
}

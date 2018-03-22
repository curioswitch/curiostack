import { Record } from 'immutable';
import { getType } from 'typesafe-actions';

import actions, { Action } from './actions';

export interface StateProps {
  sampleProp: string;
}

export type State = Readonly<StateProps> & Record<StateProps>;

export const initialState = Record<StateProps>({
  sampleProp: '',
})();

export default function(state: State, action: Action): State {
  switch (action.type) {
    case getType(actions.sampleAction):
      return state;
    default:
      return state;
  }
}

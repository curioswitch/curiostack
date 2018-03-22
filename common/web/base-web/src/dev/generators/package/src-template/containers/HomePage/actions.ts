import { bindActionCreators, Dispatch } from 'redux';
import { createAction } from 'typesafe-actions';
import { $call } from 'utility-types';

const types = {
  sampleAction: 'HomePage/SAMPLE_ACTION',
};

const actions = {
  sampleAction: createAction(types.sampleAction),
};

export type DispatchProps = typeof actions;

export function mapDispatchToProps(dispatch: Dispatch<any>): any {
  return bindActionCreators(actions, dispatch);
}

const returnsOfActions = Object.values(actions).map($call);
export type Action = typeof returnsOfActions[number];

export default actions;

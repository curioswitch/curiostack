import { all, AllEffect, takeLatest } from 'redux-saga/effects';
import { getType } from 'typesafe-actions';

import actions, { Action } from './actions';

function* doSampleAction() {
  yield '';
}

function* unionSaga(action: Action) {
  switch (action.type) {
    case getType(actions.sampleAction):
      yield doSampleAction();
      break;
    default:
  }
}

export default function* rootSaga(): IterableIterator<AllEffect> {
  yield all(
    Object.values(actions).map((action) =>
      takeLatest(getType(action), unionSaga),
    ),
  );
}

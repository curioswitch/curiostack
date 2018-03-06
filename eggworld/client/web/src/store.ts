/**
 * Create the store with dynamic reducers
 */

import { History } from 'history';

import { GlobalStateBase } from '@curiostack/base-web/redux';
import configureStore, {
  InjectableStore,
} from '@curiostack/base-web/redux/store';

import {
  initialState as homePageInitialState,
  State as HomePageState,
} from './containers/HomePage/reducer';

interface State {
  readonly homePage: HomePageState;
}

export type GlobalState = State & GlobalStateBase;

export default function setup(history: History): InjectableStore<GlobalState> {
  const initialState: State = {
    homePage: homePageInitialState,
  };

  return configureStore(initialState, history);
}

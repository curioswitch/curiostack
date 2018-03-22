import { GlobalState } from '../../app';

import { StateProps } from './reducer';

export const selectHomePage = (state: GlobalState): StateProps =>
  state.homePage.toObject();

export default selectHomePage;

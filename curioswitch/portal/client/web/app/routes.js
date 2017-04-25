// These are the pages you can go to.
// They are all wrapped in the App component, which should contain the navbar etc
// See http://blog.mxstbr.com/2016/01/react-apps-with-pages for more information
// about the code splitting business

import React from 'react';
import { Switch } from 'react-router';

import AsyncRoute from 'routing/AsyncRoute';
import createHomePageLoader from 'containers/HomePage/loader';
import createNotFoundPageLoader from 'containers/NotFoundPage/loader';

const Routes = ({ store }) => (
  <Switch>
    <AsyncRoute
      exact path="/" load={createHomePageLoader(store)}
    />
    <AsyncRoute
      exact path="" load={createNotFoundPageLoader(store)}
    />
  </Switch>
);

Routes.propTypes = {
  store: React.PropTypes.object,
};

export default Routes;

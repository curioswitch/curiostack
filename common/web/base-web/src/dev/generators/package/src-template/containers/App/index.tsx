import { injectReducer } from '@curiostack/base-web';

import React from 'react';
import Helmet from 'react-helmet';
import { hot } from 'react-hot-loader';
import { InjectedIntlProps, injectIntl } from 'react-intl';
import { connect } from 'react-redux';
import { Route, Switch } from 'react-router-dom';
import { compose, Reducer } from 'redux';

import HomePage from '../HomePage/Loadable';
import NotFoundPage from '../NotFoundPage/Loadable';

import { DispatchProps, mapDispatchToProps } from './actions';
import messages from './messages';
import reducer from './reducer';
import mapStateToProps, { SelectedProps } from './selectors';

type Props = SelectedProps & DispatchProps & InjectedIntlProps;

class App extends React.PureComponent<Props> {
  public render() {
    const {
      intl: { formatMessage: _ },
    } = this.props;
    return (
      <>
        <Helmet
          defaultTitle={_(messages.defaultTitle)}
          titleTemplate={_(messages.titleTemplate)}
        />
        <Switch>
          <Route exact path="/" component={HomePage} />
          <Route component={NotFoundPage} />
        </Switch>
      </>
    );
  }
}

const withConnect = connect(mapStateToProps, mapDispatchToProps);
const withReducer = injectReducer({
  reducer: reducer as Reducer<any>,
  key: 'app',
});

export default compose(injectIntl, withReducer, withConnect, hot(module))(App);

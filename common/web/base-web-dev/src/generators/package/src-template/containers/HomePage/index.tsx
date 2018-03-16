import injectReducer from '@curiostack/base-web/hoc/injectReducer';
import injectSaga from '@curiostack/base-web/hoc/injectSaga';

import React from 'react';
import Helmet from 'react-helmet';
import { hot } from 'react-hot-loader';
import { FormattedMessage, InjectedIntlProps, injectIntl } from 'react-intl';
import { connect } from 'react-redux';
import { compose } from 'redux';

import { DispatchProps, mapDispatchToProps } from './actions';
import messages from './messages';
import reducer from './reducer';
import saga from './saga';
import selectHomePage from './selectors';

type Props = DispatchProps & InjectedIntlProps;

class HomePage extends React.PureComponent<Props> {
  public render() {
    const { intl: { formatMessage: _ } } = this.props;
    return (
      <>
        <Helmet title={_(messages.title)} />
        <h1>
          <FormattedMessage {...messages.header} />
        </h1>
      </>
    );
  }
}

const withConnect = connect(selectHomePage, mapDispatchToProps);
const withReducer = injectReducer({
  reducer,
  key: 'homePage',
});
const withSaga = injectSaga({ saga, key: 'homePage' });

export default compose(
  injectIntl,
  withReducer,
  withSaga,
  withConnect,
  hot(module),
)(HomePage);

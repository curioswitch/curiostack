import React from 'react';
import { hot } from 'react-hot-loader';
import { FormattedMessage } from 'react-intl';

import messages from './messages';

class NotFoundPage extends React.PureComponent {
  public render() {
    return (
      <h1>
        <FormattedMessage {...messages.header} />
      </h1>
    );
  }
}

export default hot(module)(NotFoundPage);

/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import {
  injectReducer,
  injectSaga,
  LoadingIndicator,
} from '@curiostack/base-web';
import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { compose } from 'redux';

import { DispatchProps, mapDispatchToProps } from './actions';
import reducer, { StateProps } from './reducer';
import saga from './saga';
import selectPlacePage from './selectors';

interface MatchParams {
  id: string;
}

type Props = DispatchProps & RouteComponentProps<MatchParams> & StateProps;

const PlacePage: React.FunctionComponent<Props> = React.memo(
  ({ doGetPlace, match, place }) => {
    const id = match.params.id;

    useEffect(() => {
      doGetPlace(id);
    }, [id]);

    if (!place) {
      return <LoadingIndicator />;
    }

    return (
      <>
        <h1>{place.getName()}</h1>
        <a
          href={`https://www.instagram.com/explore/locations/${place.getInstagramId()}/`}
          target="_blank"
          rel="noopener noreferrer"
        >
          Instagram
        </a>
      </>
    );
  },
);
const withConnect = connect(
  selectPlacePage,
  mapDispatchToProps,
);
const withReducer = injectReducer({ reducer, key: 'placePage' });
const withSaga = injectSaga({ saga, key: 'placePage' });

export default compose(
  withReducer,
  withSaga,
  withConnect,
)(PlacePage);

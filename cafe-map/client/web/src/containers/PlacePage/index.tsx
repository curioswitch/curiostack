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
import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import SvgIcon from '@material-ui/core/SvgIcon';
import AttachMoneyIcon from '@material-ui/icons/AttachMoney';
import LocalPhoneIcon from '@material-ui/icons/LocalPhone';
import LocationOnIcon from '@material-ui/icons/LocationOn';
import TimerIcon from '@material-ui/icons/Timer';
import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { compose } from 'redux';
import styled from 'styled-components';

import { DispatchProps, mapDispatchToProps } from './actions';
import reducer, { FullPlace, StateProps } from './reducer';
import saga from './saga';
import selectPlacePage from './selectors';

interface MatchParams {
  id: string;
}

type Props = DispatchProps & RouteComponentProps<MatchParams> & StateProps;

const HeaderContainer = styled.div`
  position: relative;
`;

const HeaderImage = styled.img`
  width: 100%;
  max-height: 400px;
  object-fit: cover;
`;

const HeaderTitle = styled.h1`
  position: absolute;
  margin-left: 10px;
  bottom: 0;
  color: white;
`;

const Header: React.FunctionComponent<{ place: FullPlace }> = React.memo(
  ({ place: { place, googlePlace } }) => (
    <HeaderContainer>
      <HeaderImage
        alt={place.getName()}
        src={googlePlace.photos![0].getUrl({})}
      />
      <HeaderTitle>{place.getName()}</HeaderTitle>
    </HeaderContainer>
  ),
);

const PlaceDetail: React.FunctionComponent<{
  Icon: SvgIcon;
  text: string;
}> = React.memo(({ Icon, text }) => (
  <Grid container alignItems="center">
    <Icon />
    {text}
  </Grid>
));

const PlacePage: React.FunctionComponent<Props> = React.memo(
  ({ doGetPlace, match, place: fullPlace }) => {
    const id = match.params.id;

    useEffect(() => {
      doGetPlace(id);
    }, [id]);

    if (!fullPlace) {
      return <LoadingIndicator />;
    }

    const { place, googlePlace } = fullPlace;

    return (
      <>
        <Header place={fullPlace} />
        <Grid container>
          <Grid item xs={6}>
            <PlaceDetail
              Icon={TimerIcon}
              text={`${googlePlace.opening_hours.periods[0].open.time}-
            ${googlePlace.opening_hours.periods[0].close.time}`}
            />
          </Grid>
          <Grid item xs={6}>
            <PlaceDetail
              Icon={AttachMoneyIcon}
              text={googlePlace.price_level}
            />
          </Grid>
          <Grid item xs={6}>
            <PlaceDetail
              Icon={LocalPhoneIcon}
              text={googlePlace.formatted_phone_number}
            />
          </Grid>
          <Grid item xs={6} />
          <Grid item xs={12}>
            <PlaceDetail
              Icon={LocationOnIcon}
              text={googlePlace.formatted_address}
            />
          </Grid>
        </Grid>
        <Grid item xs={12}>
          <Button
            variant="contained"
            color="primary"
            href={`https://www.instagram.com/explore/locations/${place.getInstagramId()}/`}
            fullWidth
          >
            Instagram
          </Button>
        </Grid>
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

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

import IconButton from '@material-ui/core/IconButton';
import InputBase from '@material-ui/core/InputBase';
import Paper from '@material-ui/core/Paper';
import SearchIcon from '@material-ui/icons/Search';
import React from 'react';
import { WrappedFieldProps } from 'redux-form';
import styled from 'styled-components';

export interface SearchBoxProps {
  className?: string;
  onSearch: () => void;
}

const SearchBox: React.FunctionComponent<
  SearchBoxProps & WrappedFieldProps
> = ({ className, input, onSearch, meta: { touched, invalid } }) => (
  <Paper className={className} elevation={1}>
    <InputBase
      placeholder="Find a station"
      fullWidth
      type="search"
      endAdornment={
        <IconButton aria-label="Search" onClick={onSearch}>
          <SearchIcon />
        </IconButton>
      }
      error={touched && invalid}
      // eslint-disable-next-line react/jsx-props-no-spreading
      {...input}
    />
  </Paper>
);

const StyledSearchBox = styled(React.memo(SearchBox))`
  padding-left: 10px;
`;

export default StyledSearchBox;

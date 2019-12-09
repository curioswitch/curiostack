/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

import React from 'react';
import { ReactReduxContext, ReactReduxContextValue } from 'react-redux';
import { Reducer } from 'redux';

import getInjectors from '../state/injector';

interface Options {
  key: string;
  reducer: Reducer;
}

interface HocProps {
  reduxCtx: ReactReduxContextValue;
}

/**
 * Dynamically injects a reducer
 *
 * @param {string} key A key of the reducer
 * @param {function} reducer A reducer that will be injected
 *
 * Deprecated - this HOC will be deleted when React 17 is released. Migrate to hooks and useSaga before then.
 *
 */
export default ({ key, reducer }: Options) => <TOriginalProps extends {}>(
  WrappedComponent:
    | React.ComponentClass<TOriginalProps>
    | React.StatelessComponent<TOriginalProps>,
) => {
  class ReducerInjector extends React.Component<TOriginalProps & HocProps> {
    // eslint-disable-next-line react/static-property-placement
    public static displayName = `withReducer(${WrappedComponent.displayName ||
      WrappedComponent.name ||
      'Component'})`;

    private injectors = getInjectors(this.props.reduxCtx.store as any);

    // eslint-disable-next-line camelcase
    public UNSAFE_componentWillMount() {
      const { injectReducer } = this.injectors;
      injectReducer(key, reducer);
    }

    public render() {
      const { reduxCtx, ...others } = this.props as any;
      // eslint-disable-next-line react/jsx-props-no-spreading
      return <WrappedComponent {...others} />;
    }
  }
  return (props: TOriginalProps) => (
    <ReactReduxContext.Consumer>
      {(value) => (
        // eslint-disable-next-line react/jsx-props-no-spreading
        <ReducerInjector reduxCtx={value} {...props} />
      )}
    </ReactReduxContext.Consumer>
  );
};

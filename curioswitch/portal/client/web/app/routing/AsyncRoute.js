/**
 * A helper comonent that renders a Route that will load a component when the Route is
 * rendered. This allows for asynchronous loading of routes, enabling code-splitting.
 *
 * Inspired by https://reacttraining.com/react-router/web/guides/code-splitting
 */

import React, { Component } from 'react';
import { Route } from 'react-router';

/**
 * A wrapper component that will lazily render a component after it has been loaded.
 */
class Bundle extends Component {
  state = {
    // short for "module" but that's a keyword in js, so "mod"
    mod: null,
  };

  componentWillMount() {
    this.load(this.props);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.load !== this.props.load) {
      this.load(nextProps);
    }
  }

  load(props) {
    this.setState({
      mod: null,
    });
    props.load((mod) => {
      this.setState({
        // handle both es imports and cjs
        mod: mod.default ? mod.default : mod,
      });
    });
  }

  render() {
    // eslint-disable-next-line no-unused-vars
    const { load, ...otherProps } = this.props;
    return this.state.mod && <this.state.mod {...otherProps} />;
  }
}

Bundle.propTypes = {
  children: React.PropTypes.node,
  load: React.PropTypes.func,
};

// wrap <Route> and use this everywhere instead, then when
// sub routes are added to any route it'll work
const AsyncRoute = ({ path, load }) => (
  <Route
    path={path} render={(props) => (
      <Bundle load={load} {...props} />
  )}
  />
);

AsyncRoute.propTypes = {
  path: React.PropTypes.string,
  load: React.PropTypes.func,
};

export default AsyncRoute;

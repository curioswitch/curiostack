import LoadingIndicator from '@curiostack/base-web/components/LoadingIndicator';

// tslint:disable-next-line:no-var-requires
const Loadable = require('react-loadable');

export default Loadable({
  loader: () => import('./index'),
  loading: LoadingIndicator,
});

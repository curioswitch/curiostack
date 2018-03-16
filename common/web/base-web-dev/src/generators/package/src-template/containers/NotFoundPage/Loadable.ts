// tslint:disable-next-line:no-var-requires
const Loadable = require('react-loadable');

import LoadingIndicator from '../../components/LoadingIndicator';

export default Loadable({
  loader: () => import('./index'),
  loading: LoadingIndicator,
});

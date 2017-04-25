/*
 * Copyright (c) 2017 Instaconnect, Inc. All Rights Reserved.
 */

/**
 * Asynchronously loads the components for HomePage
 */

import { errorLoading } from 'utils/asyncInjectors';

export default () => (cb) => {
  import('containers/HomePage')
    .then(cb)
    .catch(errorLoading);
};

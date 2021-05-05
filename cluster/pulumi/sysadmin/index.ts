import './iam';
import './network';
import './publishing';

import {
  id as curiostackGithubAccessKeyId,
  secret as curiostackGithubAccessKeySecret,
} from './iam/curiostack-github';

import { result as npmPublishKey } from './publishing/npm';

export {
  curiostackGithubAccessKeyId,
  curiostackGithubAccessKeySecret,
  npmPublishKey,
};

import * as pulumi from '@pulumi/pulumi';

import './iam';
import './network';
import './publishing';

import {
  id as curiostackGithubAccessKeyId,
  secret as curiostackGithubAccessKeySecret,
} from './iam/curiostack-github';

import { result as npmPublishKey } from './publishing/npm';
import {
  mavenUsernameResult as mavenUsername,
  mavenPasswordResult as mavenPassword,
  publicKey as mavenGpgPublicKey,
  privateKey as mavenGpgPrivateKey,
} from './publishing/maven';

export {
  curiostackGithubAccessKeyId,
  curiostackGithubAccessKeySecret,
  npmPublishKey,
  mavenGpgPublicKey,
  mavenGpgPrivateKey,
  mavenUsername,
  mavenPassword,
};

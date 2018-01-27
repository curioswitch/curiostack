/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

import * as crypto from 'crypto';
import * as fs from 'fs';
import * as process from 'process';

import * as program from 'commander';
import * as inquirer from 'inquirer';
import * as yaml from 'js-yaml';

import * as packageJson from '../../package.json';

import { googleApis } from '../gcloud';

const KMS_LOCATIONS = [
  'global',
  'asia-east1',
  'asia-southeast1',
  'europe-west1',
  'us-central1',
  'us-east1',
  'us-west1',
];

const fetchWithMergeScript = `echo 'echo $$GITHUB_TOKEN' > /tmp/cloudbuild-github-pass.sh && \\
chmod +x /tmp/cloudbuild-github-pass.sh && \\
git init && \\
git fetch --no-tags --progress $_REPOSITORY_URL +$_OTHER_BRANCH_REMOTE_REF:$_OTHER_BRANCH_LOCAL_REF +$_BASE_BRANCH_REMOTE_REF:$_BASE_BRANCH_LOCAL_REF && \\
git config remote.origin.url $_REPOSITORY_URL && \\
git config user.name cloudbuild && \\
git config user.email cloudbuild@example.com && \\
git checkout -f $_OTHER_BRANCH_LOCAL_REF && \\
git merge --no-ff --no-edit --no-progress $_BASE_BRANCH_LOCAL_REF && \\
MERGE_REV=$(git rev-parse HEAD^{commit}) && \\
git checkout -f $_BASE_BRANCH_LOCAL_REF && \\
git merge --no-ff --no-edit --no-progress $$MERGE_REV && \\
MERGE_REV=$(git rev-parse HEAD^{commit}) && \\
git checkout -f $$MERGE_REV
`;

const indexJs = `/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

module.exports = require('@curiostack/cloudbuild-github');
`;

const DEFAULT_KMS_LOCATION = 'us-central1';
const DEFAULT_KMS_KEYRING = 'cloudbuild';
const DEFAULT_KMS_KEY = 'github';

const SECRET_NUM_BYTES = 10;

async function setup() {
  const ui = new inquirer.ui.BottomBar();

  const answers = await inquirer.prompt([
    {
      name: 'kms.location',
      message: 'Enter the location for the keyring used to encrypt secrets.',
      default: DEFAULT_KMS_LOCATION,
      validate: (val) => KMS_LOCATIONS.includes(val) || 'Invalid KMS location.',
      when: !program.defaults,
    },
    {
      name: 'kms.keyring',
      message:
        'Enter the name of the keyring used to encrypt secrets. ' +
        'Will be created if it does not exist.',
      default: DEFAULT_KMS_KEYRING,
      when: !program.defaults,
    },
    {
      name: 'kms.key',
      message:
        'Enter the name of the key used to encrypt secrets. ' +
        'Will be created if it does not exist.',
      default: DEFAULT_KMS_KEY,
      when: !program.defaults,
    },
    {
      name: 'repo.name',
      message: 'Enter the name of the GitHub repository.',
      validate: (val) => !!val || 'GitHub repository name is required.',
    },
    {
      name: 'repo.token',
      message:
        'Paste-in a GitHub personal access token with repo and hook permissions ' +
        '- generate at \nhttps://github.com/settings/tokens/new?scopes=repo,admin:repo_hook&description=cloudbuild\n',
      validate: (val) => !!val || 'Access token is required.',
    },
  ]);

  const {
    location = DEFAULT_KMS_LOCATION,
    keyring = DEFAULT_KMS_KEYRING,
    key = DEFAULT_KMS_KEY,
  } =
    answers.kms || {};

  const webhookSecret = crypto.randomBytes(SECRET_NUM_BYTES).toString('hex');
  const projectId = await googleApis.getProjectId();
  if (!projectId) {
    ui.log.write(
      'Could not determine project id, try setting the GCLOUD_PROJECT environment variable',
    );
    return;
  }

  ui.log.write('Setting up keys.');
  try {
    await googleApis.createKeyring(location, keyring);
  } catch (err) {
    if (err.errors[0].reason !== 'alreadyExists') {
      throw err;
    }
  }
  try {
    await googleApis.createKey(location, keyring, key);
  } catch (err) {
    if (err.errors[0].reason !== 'alreadyExists') {
      throw err;
    }
  }
  const projectNumber = await googleApis.getProjectNumber();
  await googleApis.setDecrypters(location, keyring, [
    `${projectId}@appspot.gserviceaccount.com`,
    `${projectNumber}@cloudbuild.gserviceaccount.com`,
  ]);

  ui.log.write('Encrypting secrets.');
  const encryptedWebhookSecret = await googleApis.encryptKey(
    location,
    keyring,
    key,
    webhookSecret,
  );
  const encryptedGithubToken = await googleApis.encryptKey(
    location,
    keyring,
    key,
    answers.repo.token,
  );

  const config = {
    kms: {
      location,
      keyring,
      key,
    },
    encryptedWebhookSecret,
    repos: {
      [answers.repo.name]: {
        encryptedGithubToken,
        cloudbuild: {
          steps: [
            {
              id: 'fetch-source',
              // Use gcloud image instead of git, since the latter prevents GIT_ASKPASS from working.
              name: 'gcr.io/cloud-builders/gcloud',
              entrypoint: 'bash',
              args: ['-c', fetchWithMergeScript],
              env: ['GIT_ASKPASS=/tmp/cloudbuild-github-pass.sh'],
              secretEnv: ['GITHUB_TOKEN'],
            },
            {
              id: 'build-all',
              name: 'gcr.io/$PROJECT_ID/java-cloud-builder',
              entrypoint: './gradlew',
              args: ['continuousTest', '--stacktrace', '--no-daemon'],
              env: ['CI=true'],
            },
          ],
          timeout: '60m',
          secrets: [
            {
              kmsKeyName: `projects/${projectId}/locations/${location}/keyRings/${keyring}/cryptoKeys/${key}`,
              secretEnv: {
                GITHUB_TOKEN: encryptedGithubToken,
              },
            },
          ],
        },
      },
    },
  };
  const configYaml = yaml.safeDump(config, {
    // It's easier to manage build scripts without worrying about line length.
    lineWidth: 100000,
  });

  ui.log.write('Outputting config.yml and index.js.');
  fs.writeFileSync('config.yml', configYaml);
  fs.writeFileSync('index.js', indexJs);

  ui.log.write(
    'Done! Edit config.yml with any more customizations and run deploy.',
  );
}

program
  .version(packageJson.version)
  .option('--defaults', 'Use defaults')
  .parse(process.argv);

setup().then(
  () => process.exit(),
  (err) => {
    console.error('Unexpected error.', err);
    process.exit(1);
  },
);

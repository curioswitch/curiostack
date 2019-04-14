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

import process from 'process';

import axios from 'axios';
import { spawn } from 'child-process-promise';
import program from 'commander';
import inquirer from 'inquirer';

import packageJson from '../../package.json';

import config from '../config';
import google from '../gcloud';
import { keyManager } from '../keymanager';

const GITHUB_API_BASE = 'https://api.github.com';

interface Hook {
  id?: string;
  name: string;
  config: {
    url: string;
    content_type: string;
    secret: string;
  };
  events: string[];
}

async function makeGithubRequest(
  uri: string,
  body: any,
  token: string,
  method: string = 'POST',
) {
  return axios({
    method,
    url: uri,
    data: body,
    headers: {
      'User-Agent': 'cloudbuild-github',
    },
    auth: {
      username: 'token',
      password: token,
    },
  });
}

async function deploy() {
  const ui = new inquirer.ui.BottomBar();

  const projectId = await google.auth.getProjectId();

  const webhookSecret = await keyManager.getWebhookSecret();

  ui.log.write('Deploying cloud functions.');
  if (program.delete) {
    try {
      await spawn(
        'gcloud',
        ['--quiet', 'beta', 'functions', 'delete', 'cloudbuildGithubWebhook'],
        { stdio: 'inherit' },
      );
    } catch (err) {
      ui.log.write('Could not delete cloudbuildGithubWebhook, skipping.');
    }
    try {
      await spawn(
        'gcloud',
        ['--quiet', 'beta', 'functions', 'delete', 'cloudbuildGithubNotifier'],
        { stdio: 'inherit' },
      );
    } catch (err) {
      ui.log.write('Could not delete cloudbuildGithubNotifier, skipping.');
    }
  }
  await spawn(
    'gcloud',
    [
      'beta',
      'functions',
      'deploy',
      'cloudbuildGithubWebhook',
      '--trigger-http',
      '--runtime=nodejs10',
      '--retry',
    ],
    { stdio: 'inherit' },
  );
  await spawn(
    'gcloud',
    [
      'beta',
      'functions',
      'deploy',
      'cloudbuildGithubNotifier',
      '--trigger-topic',
      'cloud-builds',
      '--runtime=nodejs10',
      '--retry',
    ],
    { stdio: 'inherit' },
  );

  const webhookUrl = `https://us-central1-${projectId}.cloudfunctions.net/cloudbuildGithubWebhook`;

  await Promise.all(
    Object.keys(config.repos).map(async (repoName) => {
      const repoToken: string = await keyManager.getGithubToken(repoName);
      ui.log.write(`Setting up repository webhook for ${repoName}`);
      const hooksUri = `${GITHUB_API_BASE}/repos/${repoName}/hooks`;
      const existingHooks = await makeGithubRequest(
        hooksUri,
        null,
        repoToken,
        'GET',
      );
      await Promise.all(
        existingHooks.data
          .filter((h: Hook) => h.config.url === webhookUrl)
          .map(async (h: Hook) =>
            makeGithubRequest(`${hooksUri}/${h.id}`, null, repoToken, 'DELETE'),
          ),
      );

      const hook: Hook = {
        name: 'web',
        config: {
          url: webhookUrl,
          // eslint-disable-next-line @typescript-eslint/camelcase
          content_type: 'json',
          secret: webhookSecret,
        },
        events: ['pull_request'],
      };
      return makeGithubRequest(hooksUri, hook, repoToken);
    }),
  );

  ui.log.write('Done!');
}

program
  .version(packageJson.version)
  .option('--delete', 'Delete existing functions first')
  .parse(process.argv);

deploy().then(
  () => process.exit(),
  (err) => {
    console.error('Unexpected error.', err);
    process.exit(1);
  },
);

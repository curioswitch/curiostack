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

// @flow

import process from 'process';

import { spawn } from 'child-process-promise';
import program from 'commander';
import inquirer from 'inquirer';
import request from 'request-promise-native';

import packageJson from '../../package.json';

import config from '../config';
import { googleApis } from '../gcloud';
import { keyManager } from '../keymanager';

const GITHUB_API_BASE = 'https://api.github.com';

async function makeGithubRequest(
  uri: string,
  body: any,
  token: string,
  method: string = 'POST',
) {
  return request({
    method,
    uri,
    body,
    json: true,
    headers: {
      'User-Agent': 'cloudbuild-github',
    },
    auth: {
      user: 'token',
      pass: token,
    },
  });
}

async function deploy() {
  const ui = new inquirer.ui.BottomBar();

  const projectId = await googleApis.getProjectId();

  const webhookSecret = keyManager.getWebhookSecret();

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
    ],
    { stdio: 'inherit' },
  );

  const webhookUrl = `https://us-central1-${projectId}.cloudfunctions.net/cloudbuildGithubWebhook`;

  await Promise.all(
    Object.keys(config.repos).map(async (repoName) => {
      const repoToken = await keyManager.getGithubToken(repoName);
      ui.log.write(`Setting up repository webhook for ${repoName}`);
      const hooksUri = `${GITHUB_API_BASE}/repos/${repoName}/hooks`;
      const existingHooks = await makeGithubRequest(
        hooksUri,
        null,
        repoToken,
        'GET',
      );
      await Promise.all(
        existingHooks
          .filter((hook) => hook.config.url === webhookUrl)
          .map((hook) =>
            makeGithubRequest(
              `${hooksUri}/${hook.id}`,
              null,
              repoToken,
              'DELETE',
            ),
          ),
      );

      const hook = {
        name: 'web',
        config: {
          url: webhookUrl,
          content_type: 'json',
          secret: webhookSecret,
        },
        events: ['pull_request'],
      };
      await makeGithubRequest(hooksUri, hook, repoToken);
    }),
  );

  ui.log.write('Done!');
}

program
  .version(packageJson.version)
  .option('-d, --delete', 'Delete existing functions first')
  .parse(process.argv);

deploy().then(
  () => process.exit(),
  (err) => {
    console.error('Unexpected error.', err);
    process.exit(1);
  },
);

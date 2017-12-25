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

// @flow

import request from 'request-promise-native';

import { keyManager } from './keymanager';

import { COMMENTS_URL_KEY, STATUSES_URL_KEY } from './constants';

function statusToState(status): string {
  switch (status) {
    case 'QUEUED':
    case 'WORKING':
      return 'pending';
    case 'SUCCESS':
      return 'success';
    case 'FAILURE':
      return 'failure';
    default:
      return 'error';
  }
}

function statusToDescription(status): string {
  switch (status) {
    case 'QUEUED':
      return 'Build queued.';
    case 'WORKING':
      return 'Build started.';
    case 'SUCCESS':
      return 'Build succeeded.';
    case 'FAILURE':
      return 'Build failed.';
    case 'INTERNAL_ERROR':
      return 'Build internal error.';
    case 'TIMEOUT':
      return 'Build timed out.';
    case 'CANCELLED':
      return 'Build cancelled.';
    default:
      return 'Build status unknown.';
  }
}

async function makeRequest(uri: string, body: any) {
  const githubToken = await keyManager.getGithubToken();
  return request({
    method: 'POST',
    uri,
    body,
    json: true,
    headers: {
      'User-Agent': 'cloudbuild-github',
    },
    auth: {
      user: 'token',
      pass: githubToken,
    },
  });
}

export async function handleBuildEvent(event: any) {
  const build = JSON.parse(
    Buffer.from(event.data.data, 'base64').toString('utf8'),
  );

  const statusesUrl = build.substitutions[STATUSES_URL_KEY];
  if (!statusesUrl) {
    return;
  }

  const status = {
    state: statusToState(build.status),
    target_url: build.logUrl,
    description: statusToDescription(build.status),
    context: 'ci/cloudbuild',
  };

  const statusResponse = await makeRequest(statusesUrl, status);
  if (!statusResponse.state) {
    throw new Error(`Failed to set status: ${JSON.stringify(statusResponse)}`);
  }

  const commentsUrl = build.substitutions[COMMENTS_URL_KEY];
  if (!commentsUrl) {
    return;
  }

  let comment;
  switch (build.status) {
    case 'QUEUED':
    case 'WORKING':
    case 'CANCELLED':
    case 'STATUS_UNKNOWN':
      return;
    case 'SUCCESS':
      comment = `Build succeded. If you have approval, you're ready to merge!\n\nLogs:\n${
        build.logUrl
      }`;
      break;
    case 'FAILURE':
      comment = `Build failed. Check the logs and try again.\n\nLogs:\n${
        build.logUrl
      }`;
      break;
    default:
      comment = `Build terminated with unknown error. You may want to retry.\n\nLogs:\n${
        build.logUrl
      }`;
      break;
  }
  const commentResponse = await makeRequest(commentsUrl, { body: comment });
  if (!commentResponse.id) {
    throw new Error(
      `Failed to set comment: ${JSON.stringify(commentResponse)}`,
    );
  }
}

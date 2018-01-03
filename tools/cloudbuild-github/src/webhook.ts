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

/* tslint:disable-next-line: no-submodule-imports */
import verify from '@octokit/webhooks/verify';
import { Response } from 'express-serve-static-core';
import { PullRequest } from 'github-webhook-event-types';
import * as HttpStatus from 'http-status-codes';

import { googleApis } from './gcloud';
import { keyManager } from './keymanager';

import config from './config';
import { COMMENTS_URL_KEY, STATUSES_URL_KEY } from './constants';
import { ICloudFunctionsRequest } from './index';

async function handlePullRequest(event: PullRequest) {
  if (event.action !== 'opened' && event.action !== 'synchronize') {
    console.log('Unhandled event action: ', event.action);
    return;
  }

  const pull = event.pull_request;
  const otherBranchRemoteRef = `refs/pull/${pull.number}/head`;
  const otherBranchLocalRef = `refs/remotes/origin/PR-${pull.number}`;
  const baseBranchRemoteRef = `refs/heads/${pull.base.ref}`;
  const baseBranchLocalRef = `refs/remotes/origin/${pull.base.ref}`;
  const repo = event.repository.full_name;

  const substitutions = {
    _REPOSITORY_URL: pull.base.repo.clone_url,
    _OTHER_BRANCH_REMOTE_REF: otherBranchRemoteRef,
    _OTHER_BRANCH_LOCAL_REF: otherBranchLocalRef,
    _BASE_BRANCH_REMOTE_REF: baseBranchRemoteRef,
    _BASE_BRANCH_LOCAL_REF: baseBranchLocalRef,
    [STATUSES_URL_KEY]: pull.statuses_url,
    [COMMENTS_URL_KEY]: pull.comments_url,
  };

  const prTag = `pr.${pull.number}`;
  const tags = [
    `repo.${event.repository.name.replace('_', '-')}`,
    `sender.${event.sender.login}`,
    prTag,
  ];

  const existingBuilds = await googleApis.listCloudbuilds(`tags="${prTag}"`);
  existingBuilds
    .filter((build) => build.status === 'QUEUED' || build.status === 'WORKING')
    .forEach((build) => {
      console.log(`Found existing build ${build.id}. Cancelling.`);
      googleApis.cancelCloudbuild(build.id);
    });
  console.log(`Starting cloud build for pull request ${pull.number}.`);
  await googleApis.startCloudbuild(
    config.repos[repo].cloudbuild,
    substitutions,
    tags,
  );
}

export async function handleWebhook(
  req: ICloudFunctionsRequest,
  res: Response,
) {
  if (
    !verify(
      await keyManager.getWebhookSecret(),
      req.rawBody.toString(),
      req.get('X-Hub-Signature'),
    )
  ) {
    console.error('Invalid signature.');
    res.status(HttpStatus.BAD_REQUEST).end();
    return;
  }

  const event = req.body;
  const eventType = req.get('X-GitHub-Event');
  switch (eventType) {
    case 'pull_request':
      await handlePullRequest(event as PullRequest);
      break;
    default:
      console.log('Unhandled event type: ', eventType);
      break;
  }
  res.status(HttpStatus.OK).end();
}

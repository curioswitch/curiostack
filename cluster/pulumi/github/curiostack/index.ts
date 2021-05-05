/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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

import * as github from '@pulumi/github';

import { sysadminStack } from '../stacks';

const curiostack = new github.Repository(
  'curiostack',
  {
    allowMergeCommit: false,
    allowRebaseMerge: false,
    allowSquashMerge: true,
    archived: false,
    deleteBranchOnMerge: false,
    description: 'Full stack to help satisfy curiosity',
    hasDownloads: true,
    hasIssues: true,
    hasProjects: true,
    hasWiki: true,
    name: 'curiostack',
    vulnerabilityAlerts: true,
  },
  {
    protect: true,
  },
);

const awsAccessKeyId = new github.ActionsSecret('curiostack-awsAccessKeyId', {
  secretName: 'AWS_ACCESS_KEY_ID',
  repository: curiostack.name,
  plaintextValue: sysadminStack.getOutput('curiostackGithubAccessKeyId'),
});

const awsSecretAccessKey = new github.ActionsSecret(
  'curiostack-awsSecretAccessKey',
  {
    secretName: 'AWS_SECRET_ACCESS_KEY',
    repository: curiostack.name,
    plaintextValue: sysadminStack.getOutput('curiostackGithubAccessKeySecret'),
  },
);

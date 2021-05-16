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

import * as openpgp from '@curiostack/pulumi-openpgp';
import * as random from '@pulumi/random';

const mavenGpgKey = new openpgp.Key('maven-gpg', {
  name: 'curiostack-gpg',
  email: 'maven@curioswitch.org',
});

const mavenUsername = new random.RandomPassword(
  'maven-username',
  {
    length: 8,
  },
  {
    protect: true,
  },
);

const mavenPassword = new random.RandomPassword(
  'maven-password',
  {
    length: 44,
  },
  {
    protect: true,
  },
);

export const { publicKey, privateKey } = mavenGpgKey;
export const mavenUsernameResult = mavenUsername.result;
export const mavenPasswordResult = mavenPassword.result;

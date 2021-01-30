/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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

import * as aws from '@pulumi/aws';
import * as pulumi from '@pulumi/pulumi';

const curiostackOrg = new aws.organizations.Organization('curiostack', {});

export const sysadminAccount = new aws.organizations.Account(
  'sysadmin',
  {
    name: 'sysadmin',
    email: 'sysadmin-ext@curioswitch.org',
    parentId: curiostackOrg.roots[0].id,
    roleName: 'Owner',
  },
  {
    ignoreChanges: ['roleName'],
  },
);

const engineeringAccount = new aws.organizations.Account('engineering', {
  name: 'engineering',
  email: 'aws-engineering@curioswitch.org',
  parentId: curiostackOrg.roots[0].id,
  roleName: 'Owner',
});

export const engineeringOwnerRole = pulumi
  .all([engineeringAccount.id, engineeringAccount.roleName])
  .apply(([id, roleName]) => `arn:aws:iam::${id}:role/${roleName}`);

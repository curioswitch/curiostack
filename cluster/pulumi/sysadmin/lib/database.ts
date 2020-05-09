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
import * as random from '@pulumi/random';

export interface DatabaseArgs extends aws.rds.ClusterArgs {}

export class DatabaseCluster extends pulumi.ComponentResource {
  private readonly masterPassword: random.RandomPassword;

  private readonly cluster: aws.rds.Cluster;

  private readonly clusterParameters: aws.rds.ClusterParameterGroup;

  constructor(
    name: string,
    args: DatabaseArgs = {},
    options: pulumi.ComponentResourceOptions = {},
  ) {
    super('curiostack:aws:DatabaseCluster', name, {}, options);

    this.masterPassword = new random.RandomPassword(
      `${name}-password`,
      {
        length: 32,
        special: false,
      },
      {
        parent: this,
      },
    );

    this.clusterParameters = new aws.rds.ClusterParameterGroup(
      `${name}-parameters`,
      {
        name,
        family: 'aurora5.6',
        parameters: [
          {
            name: 'character_set_server',
            value: 'utf8mb4',
          },
          {
            name: 'collation_server',
            value: 'utf8mb4_unicode_ci',
          },
          {
            name: 'long_query_time',
            value: '0.1',
          },
          {
            name: 'slow_query_log',
            value: '1',
          },
          {
            name: 'time_zone',
            value: 'UTC',
          },
        ],
      },
      {
        parent: this,
      },
    );

    this.cluster = new aws.rds.Cluster(
      `${name}-cluster`,
      {
        dbClusterParameterGroupName: this.clusterParameters.name,
        clusterIdentifier: name,
        engine: 'aurora',
        engineMode: 'serverless',
        engineVersion: '5.6.10a',
        masterUsername: 'root',
        masterPassword: this.masterPassword.result,
        finalSnapshotIdentifier: `${name}-final-snapshot`,
        storageEncrypted: true,
        applyImmediately: true,
        scalingConfiguration: {
          // Defaults are conservative, users must explicitly ask for more power.
          maxCapacity: 1,
          minCapacity: 1,
          secondsUntilAutoPause: 300,
        },
        ...args,
      },
      {
        parent: this,
        ignoreChanges: args.availabilityZones
          ? ['availabilityZones']
          : undefined,
      },
    );

    this.registerOutputs({
      arn: this.cluster.arn,
      endpoint: this.cluster.endpoint,
      rootPassword: this.masterPassword.result,
    });
  }
}

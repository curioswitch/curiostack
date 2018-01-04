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

import * as promisify from 'es6-promisify';
import * as google from 'googleapis';
import * as parseDuration from 'parse-duration';

export interface IBuild {
  id: string;
  status: string;
  logUrl: string;
  options: {
    substitutionOption: string;
  };
  substitutions: {
    [key: string]: string;
  };
  tags: string[];
  sourceProvenance: {
    resolvedRepoSource: {
      repoName: string;
      commitSha: string;
    };
  };
}

interface IGoogleClient {
  projectId: string;

  getGoogleProject: (
    request: {
      projectId: string;
    },
  ) => Promise<{
    projectNumber: string;
  }>;

  cloudbuild: {
    cancel: (
      request: {
        projectId: string;
        id: string;
      },
    ) => Promise<{}>;

    create: (
      request: {
        projectId: string;
        resource: IBuild;
      },
    ) => Promise<{}>;

    list: (
      request: {
        projectId: string;
        filter: string;
      },
    ) => Promise<{
      builds?: IBuild[];
    }>;
  };

  kms: {
    createKeyring: (
      request: {
        parent: string;
        keyRingId: string;
      },
    ) => Promise<{ name: string }>;

    createKey: (
      request: {
        parent: string;
        cryptoKeyId: string;
        resource: {
          purpose: string;
        };
      },
    ) => Promise<{ name: string }>;

    decrypt: (
      request: {
        name: string;
        resource: {
          ciphertext: string;
        };
      },
    ) => Promise<{ plaintext: string }>;

    encrypt: (
      request: {
        name: string;
        resource: {
          plaintext: string;
        };
      },
    ) => Promise<{ ciphertext: string }>;

    setIamPolicy: (
      request: {
        resource_: string;
        resource: {
          policy: {
            bindings: Array<{
              role: string;
              members: string[];
            }>;
          };
        };
      },
    ) => Promise<{}>;
  };
}

const MILLIS_IN_SECOND = 1000;

export class GoogleApis {
  private client?: IGoogleClient;

  public async getProjectId(): Promise<string> {
    const client = await this.init();
    return client.projectId;
  }

  public async getProjectNumber(): Promise<string> {
    const client = await this.init();

    const { projectNumber } = await client.getGoogleProject({
      projectId: client.projectId,
    });
    return projectNumber;
  }

  public async createKeyring(location: string, keyring: string) {
    const client = await this.init();

    const request = {
      parent: `projects/${client.projectId}/locations/${location}`,
      keyRingId: keyring,
    };
    const created = await client.kms.createKeyring(request);
    console.log(`Keyring ${created.name} created.`);
  }

  public async createKey(location: string, keyring: string, key: string) {
    const client = await this.init();

    const request = {
      parent: `projects/${
        client.projectId
      }/locations/${location}/keyRings/${keyring}`,
      cryptoKeyId: key,
      resource: {
        purpose: 'ENCRYPT_DECRYPT',
      },
    };
    const created = await client.kms.createKey(request);
    console.log(`Key ${created.name} created.`);
  }

  public async decryptKey(
    location: string,
    keyring: string,
    key: string,
    encryptedBase64: string,
  ): Promise<string> {
    const client = await this.init();

    const request = {
      name: `projects/${
        client.projectId
      }/locations/${location}/keyRings/${keyring}/cryptoKeys/${key}`,
      resource: {
        ciphertext: encryptedBase64,
      },
    };

    const { plaintext } = await client.kms.decrypt(request);
    return plaintext;
  }

  public async encryptKey(
    location: string,
    keyring: string,
    key: string,
    plaintext: string,
  ): Promise<string> {
    const client = await this.init();

    const request = {
      name: `projects/${
        client.projectId
      }/locations/${location}/keyRings/${keyring}/cryptoKeys/${key}`,
      resource: {
        plaintext: Buffer.from(plaintext).toString('base64'),
      },
    };

    const { ciphertext } = await client.kms.encrypt(request);
    return ciphertext;
  }

  public async setDecrypters(
    location: string,
    keyring: string,
    serviceAccounts: string[],
  ) {
    const client = await this.init();

    const request = {
      resource_: `projects/${
        client.projectId
      }/locations/${location}/keyRings/${keyring}`,
      resource: {
        policy: {
          bindings: [
            {
              members: serviceAccounts.map((acc) => `serviceAccount:${acc}`),
              role: 'roles/cloudkms.cryptoKeyDecrypter',
            },
          ],
        },
      },
    };
    await client.kms.setIamPolicy(request);
  }

  public async cancelCloudbuild(id: string) {
    const client = await this.init();

    const request = {
      id,
      projectId: client.projectId,
    };
    await client.cloudbuild.cancel(request);
  }

  public async listCloudbuilds(filter: string): Promise<IBuild[]> {
    const client = await this.init();

    const request = {
      filter,
      projectId: client.projectId,
    };
    const { builds } = await client.cloudbuild.list(request);
    return builds || [];
  }

  public async startCloudbuild(
    cloudbuildConfig: any,
    substitutions: any,
    tags: string[],
  ) {
    const client = await this.init();

    const sanitizedConfig = cloudbuildConfig.timeout
      ? {
          ...cloudbuildConfig,
          timeout: `${parseDuration(cloudbuildConfig.timeout) /
            MILLIS_IN_SECOND}s`,
        }
      : cloudbuildConfig;
    const request = {
      projectId: client.projectId,
      resource: {
        ...sanitizedConfig,
        options: {
          ...sanitizedConfig.options,
          substitutionOption: 'ALLOW_LOOSE',
        },
        substitutions,
        tags,
      },
    };

    console.log('Creating build job.');
    await client.cloudbuild.create(request);
  }

  private async init(): Promise<IGoogleClient> {
    if (this.client) {
      return this.client;
    }

    const { auth, projectId } = await this.authorize();

    const cloudkms = google.cloudkms({ version: 'v1', auth });
    const cloudbuild = google.cloudbuild({ version: 'v1', auth });
    const resourceManager = google.cloudresourcemanager({
      auth,
      version: 'v1',
    });
    return {
      getGoogleProject: promisify(
        resourceManager.projects.get,
        resourceManager,
      ),
      cloudbuild: {
        cancel: promisify(cloudbuild.projects.builds.cancel, cloudbuild),
        create: promisify(cloudbuild.projects.builds.create, cloudbuild),
        list: promisify(cloudbuild.projects.builds.list, cloudbuild),
      },
      kms: {
        createKey: promisify(
          cloudkms.projects.locations.keyRings.cryptoKeys.create,
          cloudkms,
        ),
        createKeyring: promisify(
          cloudkms.projects.locations.keyRings.create,
          cloudkms,
        ),
        decrypt: promisify(
          cloudkms.projects.locations.keyRings.cryptoKeys.decrypt,
          cloudkms,
        ),
        encrypt: promisify(
          cloudkms.projects.locations.keyRings.cryptoKeys.encrypt,
          cloudkms,
        ),
        setIamPolicy: promisify(
          cloudkms.projects.locations.keyRings.cryptoKeys.setIamPolicy,
          cloudkms,
        ),
      },
      projectId,
    };
  }

  private async authorize(): Promise<{ auth: any; projectId: string }> {
    return new Promise<{ auth: any; projectId: string }>((resolve, reject) =>
      google.auth.getApplicationDefault(
        (err: Error, authClient: any, projectId: string) => {
          if (err) {
            reject(err);
            return;
          }
          resolve({
            auth: authClient,
            projectId,
          });
        },
      ),
    );
  }
}

export const googleApis = new GoogleApis();

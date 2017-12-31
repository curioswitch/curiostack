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

import promisify from 'es6-promisify';
import google from 'googleapis';
import parseDuration from 'parse-duration';

type CreateBuildRequest = any;
type CreateBuildResponse = any;

type DecryptKmsRequest = {
  name: string,
  resource: {
    ciphertext: string,
  },
};

type DecryptKmsResult = {
  plaintext: string,
};

class GoogleApis {
  projectId: ?string = null;

  cancelBuild: ?(any) => Promise<any> = null;
  createBuild: ?(CreateBuildRequest) => Promise<CreateBuildResponse> = null;
  listBuilds: ?(any) => Promise<any> = null;

  createKmsKeyring = null;
  createKmsKey = null;
  decryptKms: ?(DecryptKmsRequest) => Promise<DecryptKmsResult> = null;
  encryptKms = null;
  setKmsIamPolicy = null;

  async getProjectId() {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized.');
    }
    return this.projectId;
  }

  async createKeyring(location: string, keyring: string) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized.');
    }
    const request = {
      parent: `projects/${this.projectId}/locations/${location}`,
      keyRingId: keyring,
    };
    if (!this.createKmsKeyring) {
      throw new Error('Not initialized.');
    }
    const created = await this.createKmsKeyring(request);
    console.log(`Keyring ${created.name} created.`);
  }

  async createKey(location: string, keyring: string, key: string) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized.');
    }
    const request = {
      parent: `projects/${
        this.projectId
      }/locations/${location}/keyRings/${keyring}`,
      cryptoKeyId: key,
      resource: {
        purpose: 'ENCRYPT_DECRYPT',
      },
    };
    if (!this.createKmsKey) {
      throw new Error('Not initialized.');
    }
    const created = await this.createKmsKey(request);
    console.log(`Key ${created.name} created.`);
  }

  async decryptKey(
    location: string,
    keyring: string,
    key: string,
    encryptedBase64: string,
  ) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized.');
    }

    const request = {
      name: `projects/${
        this.projectId
      }/locations/${location}/keyRings/${keyring}/cryptoKeys/${key}`,
      resource: {
        ciphertext: encryptedBase64,
      },
    };

    if (!this.decryptKms) {
      throw new Error('Not initialized.');
    }
    const { plaintext } = await this.decryptKms(request);
    return plaintext;
  }

  async encryptKey(
    location: string,
    keyring: string,
    key: string,
    plaintext: string,
  ) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized.');
    }

    const request = {
      name: `projects/${
        this.projectId
      }/locations/${location}/keyRings/${keyring}/cryptoKeys/${key}`,
      resource: {
        plaintext: Buffer.from(plaintext).toString('base64'),
      },
    };

    if (!this.encryptKms) {
      throw new Error('Not initialized.');
    }
    const { ciphertext } = await this.encryptKms(request);
    return ciphertext;
  }

  async setDecrypter(
    location: string,
    keyring: string,
    serviceAccount: string,
  ) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized.');
    }

    const request = {
      resource_: `projects/${
        this.projectId
      }/locations/${location}/keyRings/${keyring}`,
      resource: {
        policy: {
          bindings: [
            {
              role: 'roles/cloudkms.cryptoKeyDecrypter',
              members: [`serviceAccount:${serviceAccount}`],
            },
          ],
        },
      },
    };
    if (!this.setKmsIamPolicy) {
      throw new Error('Not initialized.');
    }
    await this.setKmsIamPolicy(request);
  }

  async cancelCloudbuild(id: string) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized, this is a programming bug.');
    }
    const request = {
      projectId: this.projectId,
      id,
    };
    if (!this.cancelBuild) {
      throw new Error('Not initialized, this is a programming bug.');
    }
    return this.cancelBuild(request);
  }

  async listCloudbuilds(filter: string) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized, this is a programming bug.');
    }
    const request = {
      projectId: this.projectId,
      filter,
    };
    if (!this.listBuilds) {
      throw new Error('Not initialized, this is a programming bug.');
    }
    const { builds } = await this.listBuilds(request);
    return builds || [];
  }

  async startCloudbuild(
    cloudbuildConfig: any,
    substitutions: any,
    tags: string[],
  ) {
    await this.init();

    if (!this.projectId) {
      throw new Error('Not initialized, this is a programming bug.');
    }

    const sanitizedConfig = cloudbuildConfig.timeout
      ? {
          ...cloudbuildConfig,
          timeout: `${parseDuration(cloudbuildConfig.timeout) / 1000}s`,
        }
      : cloudbuildConfig;
    const request = {
      projectId: this.projectId,
      resource: {
        ...sanitizedConfig,
        options: {
          substitutionOption: 'ALLOW_LOOSE',
        },
        substitutions,
        tags,
      },
    };

    console.log('Creating build job.');
    if (!this.createBuild) {
      throw new Error('Not initialized, this is a programming bug.');
    }
    const response = await this.createBuild(request);
    console.log(response);
    return response;
  }

  async init() {
    if (this.projectId) {
      return;
    }

    const { auth, projectId } = await this.authorize();
    this.projectId = projectId;
    const cloudkms = google.cloudkms({ version: 'v1', auth });
    this.createKmsKeyring = promisify(
      cloudkms.projects.locations.keyRings.create,
      cloudkms,
    );
    this.createKmsKey = promisify(
      cloudkms.projects.locations.keyRings.cryptoKeys.create,
      cloudkms,
    );
    this.decryptKms = promisify(
      cloudkms.projects.locations.keyRings.cryptoKeys.decrypt,
      cloudkms,
    );
    this.encryptKms = promisify(
      cloudkms.projects.locations.keyRings.cryptoKeys.encrypt,
      cloudkms,
    );
    this.setKmsIamPolicy = promisify(
      cloudkms.projects.locations.keyRings.cryptoKeys.setIamPolicy,
      cloudkms,
    );

    const cloudbuild = google.cloudbuild({ version: 'v1', auth });
    this.cancelBuild = promisify(cloudbuild.projects.builds.cancel, cloudbuild);
    this.createBuild = promisify(cloudbuild.projects.builds.create, cloudbuild);
    this.listBuilds = promisify(cloudbuild.projects.builds.list, cloudbuild);
  }

  async authorize() {
    return new Promise((resolve, reject) =>
      google.auth.getApplicationDefault((err, authClient, projectId) => {
        if (err) {
          reject(err);
          return;
        }
        resolve({
          auth: authClient,
          projectId,
        });
      }),
    );
  }
}

export const googleApis = new GoogleApis();

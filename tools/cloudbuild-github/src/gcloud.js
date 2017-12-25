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
  decryptKms: ?(DecryptKmsRequest) => Promise<DecryptKmsResult> = null;

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
    this.decryptKms = promisify(
      cloudkms.projects.locations.keyRings.cryptoKeys.decrypt,
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

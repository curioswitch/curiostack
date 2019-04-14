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

import { GoogleApis } from 'googleapis';

import config from './config';

const google = new GoogleApis();

export class KeyManager {
  private decryptedKeys: Map<string, string> = new Map();

  public async getGithubToken(repo: string): Promise<string> {
    return this.getDecrypted(
      config.repos[repo].encryptedGithubToken,
      `GITHUB_TOKEN-${repo}`,
    );
  }

  public async getWebhookSecret(): Promise<string> {
    return this.getDecrypted(config.encryptedWebhookSecret, 'WEBHOOK_SECRET');
  }

  private async getDecrypted(encryptedBase64: string, cacheKey: string) {
    const cached = this.decryptedKeys.get(cacheKey);
    if (cached) {
      return cached;
    }
    const projectId = await google.auth.getProjectId();
    console.log('Decrypting ', cacheKey);

    const response = await google
      .cloudkms({ version: 'v1' })
      .projects.locations.keyRings.cryptoKeys.decrypt({
        name: `projects/${projectId}/locations/${
          config.kms.location
        }/keyRings/${config.kms.keyring}/cryptoKeys/${config.kms.key}`,
        requestBody: {
          ciphertext: encryptedBase64,
        },
      });

    const decrypted = Buffer.from(response.data!.plaintext!, 'base64').toString(
      'ascii',
    );
    this.decryptedKeys.set(cacheKey, decrypted);
    return decrypted;
  }
}

export const keyManager = new KeyManager();

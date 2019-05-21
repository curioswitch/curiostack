/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.gcloud.keys;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import java.util.Base64;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.gradle.api.Project;

public class KmsKeyDecrypter {

  private final Project project;

  public KmsKeyDecrypter(Project project) {
    this.project = project;
  }

  public String decrypt(String cipherText) throws Exception {
    var config = project.getRootProject().getExtensions().getByType(GcloudExtension.class);
    // TODO(choko): This works because the root project is always evaluated first. It's unclear
    // whether we should rely on this behavior, though, or change this to return a Provider
    String project = config.getClusterProject().get() + "-sysadmin";

    var key = CryptoKeyName.of(project, "global", "gradle-key-ring", "gradle-vars-key");

    try (var client = KeyManagementServiceClient.create()) {
      var decryptResponse =
          client.decrypt(key, ByteString.copyFrom(Base64.getDecoder().decode(cipherText)));
      return decryptResponse.getPlaintext().toStringUtf8();
    }
  }
}

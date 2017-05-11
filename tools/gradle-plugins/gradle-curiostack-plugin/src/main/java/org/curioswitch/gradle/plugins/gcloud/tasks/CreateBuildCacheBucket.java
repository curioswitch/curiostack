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

package org.curioswitch.gradle.plugins.gcloud.tasks;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Domain;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableGcloudExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class CreateBuildCacheBucket extends DefaultTask {

  @TaskAction
  public void exec() {
    ImmutableGcloudExtension config = getProject().getExtensions().getByType(GcloudExtension.class);

    Storage cloudStorage =
        StorageOptions.newBuilder().setProjectId(config.clusterProject()).build().getService();

    cloudStorage
        .create(BucketInfo.of(config.buildCacheStorageBucket()))
        .createAcl(Acl.of(new Domain("istellar.jp"), Role.READER));
  }
}

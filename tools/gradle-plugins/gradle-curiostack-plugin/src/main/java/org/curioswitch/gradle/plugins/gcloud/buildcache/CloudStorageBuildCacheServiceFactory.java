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

package org.curioswitch.gradle.plugins.gcloud.buildcache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.BuildCacheServiceFactory;

public class CloudStorageBuildCacheServiceFactory
    implements BuildCacheServiceFactory<CloudStorageBuildCache> {

  @Override
  public BuildCacheService createBuildCacheService(
      CloudStorageBuildCache buildCache, Describer describer) {
    checkNotNull(buildCache.getBucket(), "buildCache.bucket");

    describer.type("Google Cloud Storage Build Cache").config("bucket", buildCache.getBucket());

    Storage cloudStorage =
        StorageOptions.newBuilder().setProjectId(buildCache.getProject()).build().getService();

    return new CloudStorageBuildCacheService(cloudStorage, buildCache.getBucket());
  }
}

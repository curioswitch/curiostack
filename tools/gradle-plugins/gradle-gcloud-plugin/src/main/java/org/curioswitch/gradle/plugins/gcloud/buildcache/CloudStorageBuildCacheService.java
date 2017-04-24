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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import org.gradle.caching.BuildCacheEntryReader;
import org.gradle.caching.BuildCacheEntryWriter;
import org.gradle.caching.BuildCacheException;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.BuildCacheService;

public class CloudStorageBuildCacheService implements BuildCacheService {

  private static final String BUILD_CACHE_CONTENT_TYPE =
      "application/vnd.gradle.build-cache-artifact";

  private final Storage cloudStorage;
  private final String bucket;

  public CloudStorageBuildCacheService(Storage cloudStorage, String bucket) {
    this.cloudStorage = cloudStorage;
    this.bucket = bucket;
  }

  @Override
  public boolean load(BuildCacheKey buildCacheKey, BuildCacheEntryReader buildCacheEntryReader)
      throws BuildCacheException {
    Blob blob = cloudStorage.get(cacheKeyToBlobId(buildCacheKey));
    if (blob == null || !blob.exists()) {
      return false;
    }
    try (InputStream is = Channels.newInputStream(blob.reader())) {
      buildCacheEntryReader.readFrom(is);
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading file from cloud storage.", e);
    }
    return true;
  }

  @Override
  public void store(BuildCacheKey buildCacheKey, BuildCacheEntryWriter buildCacheEntryWriter)
      throws BuildCacheException {
    Blob blob = cloudStorage.get(cacheKeyToBlobId(buildCacheKey));
    if (blob == null || !blob.exists()) {
      blob =
          cloudStorage.create(
              BlobInfo.newBuilder(cacheKeyToBlobId(buildCacheKey))
                  .setContentType(BUILD_CACHE_CONTENT_TYPE)
                  .build());
    }
    try (OutputStream os = Channels.newOutputStream(blob.writer())) {
      buildCacheEntryWriter.writeTo(os);
    } catch (IOException e) {
      throw new UncheckedIOException("Error writing file from cloud storage.", e);
    }
  }

  private BlobId cacheKeyToBlobId(BuildCacheKey buildCacheKey) {
    return BlobId.of(bucket, buildCacheKey.getHashCode());
  }

  @Override
  public String getDescription() {
    return String.format("Google Cloud Storage Build Cache (%s)", bucket);
  }

  @Override
  public void close() throws IOException {}
}

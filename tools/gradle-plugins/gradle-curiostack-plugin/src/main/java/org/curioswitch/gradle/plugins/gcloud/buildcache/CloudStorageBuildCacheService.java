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

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import java.io.IOException;
import org.curioswitch.curiostack.gcloud.storage.FileWriter;
import org.curioswitch.curiostack.gcloud.storage.StorageClient;
import org.gradle.caching.BuildCacheEntryReader;
import org.gradle.caching.BuildCacheEntryWriter;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.BuildCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudStorageBuildCacheService implements BuildCacheService {

  private static final Logger logger = LoggerFactory.getLogger(CloudStorageBuildCacheService.class);

  private final StorageClient cloudStorage;

  CloudStorageBuildCacheService(StorageClient cloudStorage) {
    this.cloudStorage = cloudStorage;
  }

  @Override
  public boolean load(BuildCacheKey buildCacheKey, BuildCacheEntryReader buildCacheEntryReader) {
    ByteBuf data = cloudStorage.readFile(buildCacheKey.getHashCode()).join();
    if (data == null) {
      return false;
    }
    try (ByteBufInputStream s = new ByteBufInputStream(data)) {
      buildCacheEntryReader.readFrom(s);
    } catch (IOException e) {
      logger.warn("Exception processing cloud storage data.", e);
      return false;
    } finally {
      data.release();
    }
    return true;
  }

  @Override
  public void store(BuildCacheKey buildCacheKey, BuildCacheEntryWriter buildCacheEntryWriter) {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer((int) buildCacheEntryWriter.getSize());
    boolean success = false;
    try {
      try (ByteBufOutputStream os = new ByteBufOutputStream(buf)) {
        buildCacheEntryWriter.writeTo(os);
      } catch (IOException e) {
        logger.warn("Couldn't write cache entry to buffer.", e);
        return;
      }

      FileWriter writer =
          cloudStorage.createFile(buildCacheKey.getHashCode(), ImmutableMap.of()).join();
      writer.writeAndClose(buf).join();
      success = true;
    } finally {
      if (!success) {
        buf.release();
      }
    }
  }

  @Override
  public void close() throws IOException {}
}

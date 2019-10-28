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

import static com.google.common.util.concurrent.Futures.getUnchecked;

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
    final ByteBuf data;
    try {
      data = cloudStorage.readFile(buildCacheKey.getHashCode()).join();
    } catch (Throwable t) {
      logger.warn("Exception reading from build cache.", t);
      return false;
    }
    if (data == null) {
      return false;
    }
    try (ByteBufInputStream s = new ByteBufInputStream(data, true)) {
      buildCacheEntryReader.readFrom(s);
    } catch (Throwable t) {
      logger.warn("Exception processing cloud storage data.", t);
      return false;
    }
    return true;
  }

  @Override
  public void store(BuildCacheKey buildCacheKey, BuildCacheEntryWriter buildCacheEntryWriter) {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer((int) buildCacheEntryWriter.getSize());

    try {
      try (ByteBufOutputStream os = new ByteBufOutputStream(buf)) {
        buildCacheEntryWriter.writeTo(os);
      } catch (IOException e) {
        logger.warn("Couldn't write cache entry to buffer.", e);
        buf.release();
        return;
      }

      FileWriter writer =
          cloudStorage.createFile(buildCacheKey.getHashCode(), ImmutableMap.of()).join();
      while (buf.readableBytes() > 0) {
        ByteBuf chunk = buf.readRetainedSlice(Math.min(buf.readableBytes(), 10 * 4 * 256 * 1000));
        if (buf.readableBytes() > 0) {
          getUnchecked(writer.write(chunk));
        } else {
          writer.writeAndClose(chunk).join();
        }
      }
    } catch (Throwable t) {
      logger.warn("Exception writing to cloud storage, ignoring.", t);
    } finally {
      buf.release();
    }
  }

  @Override
  public void close() throws IOException {}
}

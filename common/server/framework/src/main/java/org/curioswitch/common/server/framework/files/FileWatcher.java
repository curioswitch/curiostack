/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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
package org.curioswitch.common.server.framework.files;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A watcher of filesystem events for registered {@link Path}s. All {@link Path}s are watched on a
 * single watcher {@link Thread}.
 */
public class FileWatcher implements AutoCloseable {

  private static final Logger logger = LogManager.getLogger();

  @Singleton
  public static class Builder {
    private final Map<Path, Consumer<Path>> registeredPaths;

    @Inject
    public Builder() {
      this.registeredPaths = new HashMap<>();
    }

    public Builder registerPath(Path path, Consumer<Path> callback) {
      registeredPaths.put(path, callback);
      return this;
    }

    public boolean isEmpty() {
      return registeredPaths.isEmpty();
    }

    public FileWatcher build() {
      return new FileWatcher(registeredPaths);
    }
  }

  private final WatchService watchService;
  private final ExecutorService executor;
  private final Map<Path, Consumer<Path>> registeredPaths;
  private final Map<WatchKey, Path> watchedDirs;

  private FileWatcher(Map<Path, Consumer<Path>> registeredPaths) {
    this.registeredPaths = ImmutableMap.copyOf(registeredPaths);
    try {
      watchService = FileSystems.getDefault().newWatchService();
      ImmutableMap.Builder<WatchKey, Path> watchedDirsBuilder = ImmutableMap.builder();
      for (Map.Entry<Path, Consumer<Path>> entry : registeredPaths.entrySet()) {
        Path dir = entry.getKey().getParent();
        WatchKey key =
            dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        watchedDirsBuilder.put(key, dir);
      }
      this.watchedDirs = watchedDirsBuilder.build();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not create WatchService.", e);
    }
    executor = Executors.newSingleThreadScheduledExecutor();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public void start() {
    executor.submit(this::listenForFileEvents);
  }

  private void listenForFileEvents() {
    for (; ; ) {
      final WatchKey key;
      try {
        key = watchService.take();
      } catch (ClosedWatchServiceException | InterruptedException ex) {
        return;
      }

      key.pollEvents()
          .stream()
          .filter(e -> e.kind() != StandardWatchEventKinds.OVERFLOW)
          .map(
              e -> {
                @SuppressWarnings("unchecked") // Only support Path
                WatchEvent<Path> cast = (WatchEvent<Path>) e;
                return cast.context();
              })
          .forEach(
              path -> {
                final Path resolved = watchedDirs.get(key).resolve(path);
                Optional<Consumer<Path>> callback =
                    registeredPaths
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().equals(resolved))
                        .map(Entry::getValue)
                        .findFirst();
                if (callback.isPresent()) {
                  logger.info("Processing update to path: " + resolved);
                  try {
                    callback.get().accept(resolved);
                  } catch (Exception e) {
                    logger.warn("Unexpected exception processing update to path: {}", resolved, e);
                  }
                } else {
                  logger.info("Could not find callback for path: {}", resolved);
                }
              });

      boolean valid = key.reset();
      if (!valid) {
        break;
      }
    }
  }

  @Override
  public void close() {
    try {
      watchService.close();
    } catch (IOException e) {
      throw new UncheckedIOException("Error shutting down WatchService.", e);
    }
    executor.shutdown();
  }
}

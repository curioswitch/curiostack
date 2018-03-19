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

package org.curioswitch.common.testing.snapshot;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.toConcurrentMap;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/** Manager of snapshots for all tests being executed. */
enum SnapshotManager {
  INSTANCE;

  private static final String UPDATE_SNAPSHOTS_SYSTEM_PROPERTY =
      "org.curioswitch.testing.updateSnapshots";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .setSerializationInclusion(Include.ALWAYS)
          .registerModule(new ProtobufModule())
          .findAndRegisterModules();

  private static final String SNAPSHOT_FILE_EXTENSION = ".snap.json";

  private static final Splitter PATH_SPLITTER = Splitter.on('/').omitEmptyStrings();

  private static final LoadingCache<Class<?>, Map<String, String>> SNAPSHOTS =
      Caffeine.newBuilder().build(SnapshotManager::loadSnapshotFile);

  void writeSnapshots() {
    SNAPSHOTS
        .asMap()
        .forEach(
            (cls, snapshots) -> {
              Path path = getSnapshotPath(cls);
              Map<String, JsonNode> serialized =
                  snapshots
                      .entrySet()
                      .stream()
                      .map(
                          entry -> {
                            try {
                              return Map.entry(
                                  entry.getKey(), OBJECT_MAPPER.readTree(entry.getValue()));
                            } catch (IOException e) {
                              throw new UncheckedIOException(
                                  "Could not serialize snapshot, can't happen.", e);
                            }
                          })
                      .collect(toImmutableMap(Entry::getKey, Entry::getValue));
              try {
                OBJECT_MAPPER.writeValue(path.toFile(), serialized);
              } catch (IOException e) {
                throw new UncheckedIOException("Could not write snapshot file.", e);
              }
            });
  }

  String getTestSnapshot(Class<?> testClass, String testName, int snapshotIndex, Object reference) {
    checkArgument(
        OBJECT_MAPPER.canSerialize(reference.getClass()),
        "Only JSON serializable objects are supported for snapshot testing.");
    Map<String, String> snapshot = SNAPSHOTS.get(testClass);
    // loadSnapshotFile never returns null.
    checkNotNull(snapshot);
    return snapshot.computeIfAbsent(
        testName + '/' + String.valueOf(snapshotIndex),
        (unused) -> {
          try {
            return OBJECT_MAPPER.writeValueAsString(reference);
          } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Could not serialize reference, can't happen.", e);
          }
        });
  }

  private static Map<String, String> loadSnapshotFile(Class<?> cls) {
    if (System.getProperty(UPDATE_SNAPSHOTS_SYSTEM_PROPERTY, "false").equals("true")) {
      return new ConcurrentHashMap<>();
    }
    Path path = getSnapshotPath(cls);
    if (!Files.exists(path)) {
      return new ConcurrentHashMap<>();
    }
    final JsonNode snapshot;
    try {
      snapshot = OBJECT_MAPPER.readTree(path.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read snapshot file.", e);
    }
    return Streams.stream(snapshot.fields())
        .map(
            field -> {
              try {
                return Map.entry(
                    field.getKey(), OBJECT_MAPPER.writeValueAsString(field.getValue()));
              } catch (JsonProcessingException e) {
                throw new UncheckedIOException("Could not reserialize JSON, this can't happen.", e);
              }
            })
        .collect(toConcurrentMap(Entry::getKey, Entry::getValue));
  }

  private static Path getSnapshotPath(Class<?> cls) {
    // This should generally be the sourceset, but keep an eye out for weird configurations where
    // it isn't.
    String sourceSet =
        Iterables.getLast(
            PATH_SPLITTER.splitToList(
                cls.getProtectionDomain().getCodeSource().getLocation().getPath()));
    // Assume working directory is the project root, which is almost always true when running
    // tests. As snapshots are only written with user intervention, it is reasonable to expect
    // users to follow this requirement.
    return Paths.get(
        "src/"
            + sourceSet
            + "/java/"
            + cls.getPackageName().replace('.', '/')
            + '/'
            + cls.getSimpleName()
            + SNAPSHOT_FILE_EXTENSION);
  }
}

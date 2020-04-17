/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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
import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.Message;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.opentest4j.AssertionFailedError;

public final class SnapshotAssertions {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .setSerializationInclusion(Include.ALWAYS)
          .registerModule(new ProtobufModule())
          .findAndRegisterModules();

  private enum AssertionIndexKey {
    INSTANCE
  }

  public static void assertSnapshotMatches(Object obj) {
    checkArgument(OBJECT_MAPPER.canSerialize(obj.getClass()), "obj must be JSON serializable");
    ExtensionContext ctx = SnapshotExtension.CURRENT_CONTEXT.get();
    checkNotNull(
        ctx,
        "Either SnapshotExtension is not registered or the current assertion "
            + "was made on a different thread than the test thread.");
    Store store = ctx.getStore(Namespace.create(SnapshotAssertions.class, ctx.getUniqueId()));
    int currentIndex =
        store.getOrComputeIfAbsent(AssertionIndexKey.INSTANCE, (unused) -> 0, Integer.class);
    store.put(AssertionIndexKey.INSTANCE, currentIndex + 1);
    String prefix =
        ctx.getRequiredTestClass()
            .getCanonicalName()
            .substring(ctx.getRequiredTestClass().getPackageName().length() + 1);
    String snapshot =
        SnapshotManager.INSTANCE.getTestSnapshot(
            getTopLevelClass(ctx),
            prefix + '.' + ctx.getRequiredTestMethod().getName(),
            currentIndex,
            obj);
    final Object snapshotObj;
    try {
      snapshotObj = OBJECT_MAPPER.readValue(snapshot, obj.getClass());
    } catch (IOException e) {
      throw new AssertionError("Snapshot does not match type of asserted object.");
    }
    if (obj.equals(snapshotObj)) {
      return;
    }

    if (obj instanceof Message) {
      assertThat((Message) obj)
          .usingDoubleTolerance(0.0000001)
          .usingFloatTolerance(0.00001f)
          .isEqualTo(snapshotObj);

      return;
    }

    final String serialized;
    try {
      serialized = OBJECT_MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException("Could not serialize obj, can't happen.", e);
    }
    if (serialized.equals(snapshot)) {
      return;
    }
    // TODO(choko): Include both object differences and JSON differences in error output maybe for
    // better IDE integration.
    try {
      assertEquals(snapshot, serialized);
    } catch (AssertionFailedError e) {
      throw new AssertionFailedError(
          "Snapshot doesn't match. If the diff is expected, run with updateSnapshots set. "
              + e.getMessage(),
          e.getExpected(),
          e.getActual());
    }
  }

  private static Class<?> getTopLevelClass(ExtensionContext ctx) {
    while (ctx.getParent().isPresent() && ctx.getParent().get().getTestClass().isPresent()) {
      ctx = ctx.getParent().get();
    }
    return ctx.getRequiredTestClass();
  }

  private SnapshotAssertions() {}
}

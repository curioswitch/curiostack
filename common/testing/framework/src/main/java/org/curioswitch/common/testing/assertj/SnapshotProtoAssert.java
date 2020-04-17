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
package org.curioswitch.common.testing.assertj;

import static org.curioswitch.common.testing.snapshot.SnapshotAssertions.assertSnapshotMatches;

import com.google.protobuf.Message;
import javax.annotation.Nullable;
import org.curioswitch.common.testing.assertj.proto.FluentEqualityConfig;
import org.curioswitch.common.testing.assertj.proto.ProtoAssert;

public class SnapshotProtoAssert<ACTUAL extends Message>
    extends ProtoAssert<ACTUAL, SnapshotProtoAssert<ACTUAL>> {

  SnapshotProtoAssert(@Nullable ACTUAL actual, FluentEqualityConfig config) {
    super(actual, config);
  }

  public SnapshotProtoAssert<ACTUAL> matchesSnapshot() {
    assertSnapshotMatches(actual);
    return this;
  }

  @Override
  protected SnapshotProtoAssert<ACTUAL> usingConfig(FluentEqualityConfig newConfig) {
    SnapshotProtoAssert<ACTUAL> newAssert = new SnapshotProtoAssert<>(actual, newConfig);
    if (info.hasDescription()) {
      newAssert.info.description(info.description());
    }
    return newAssert;
  }
}

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

import com.google.auto.service.AutoService;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A Jupiter extension to read and write snapshots for use with snapshot assertions. All assertions
 * must be made from the test thread.
 */
@AutoService(Extension.class)
public class SnapshotExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback {

  static final ThreadLocal<ExtensionContext> CURRENT_CONTEXT = new ThreadLocal<>();

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    CURRENT_CONTEXT.remove();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    CURRENT_CONTEXT.set(context);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (context.getParent().isPresent() && context.getParent().get().getTestClass().isPresent()) {
      return;
    }
    SnapshotManager.INSTANCE.writeSnapshots();
  }
}

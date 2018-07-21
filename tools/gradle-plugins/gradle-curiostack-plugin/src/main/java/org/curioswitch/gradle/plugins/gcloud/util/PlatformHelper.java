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

package org.curioswitch.gradle.plugins.gcloud.util;

import java.util.Properties;
import org.apache.tools.ant.taskdefs.condition.Os;

public class PlatformHelper {

  private final Properties props;

  public PlatformHelper() {
    this.props = System.getProperties();
  }

  public boolean isWindows() {
    return Os.isFamily(Os.FAMILY_WINDOWS);
  }

  public String getOsName() {
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
      return "windows";
    } else if (Os.isFamily(Os.FAMILY_MAC)) {
      return "darwin";
    } else if (Os.isFamily(Os.FAMILY_UNIX)) {
      // Assume linux version works on any unix until told otherwise.
      return "linux";
    }

    throw new IllegalArgumentException("Unsupported OS.");
  }

  public String getOsArch() {
    final String arch = props.getProperty("os.arch").toLowerCase();
    if (arch.contains("arm")) {
      return "arm";
    } else if (arch.contains("64")) {
      return "x86_64";
    }
    return "x86";
  }
}

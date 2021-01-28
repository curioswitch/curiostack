/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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
package org.curioswitch.gradle.documentation.template;

import org.curioswitch.gradle.documentation.text.TextScanner;

class TaggableText {
  private String text;

  public TaggableText(String text) {
    this.text = text;
  }

  public TaggableText allAfterTaggedLine(String tag) {
    return allAfterTaggedLine(tag, false);
  }

  public TaggableText allAfterTaggedLine(String tag, boolean useRegex) {
    var scanner = new TextScanner(text);
    this.text = useRegex
        ? scanner.getAllAfterLineMatching(tag)
        : scanner.getAllAfterLineContaining(tag);
    return this;
  }

  public TaggableText allBetweenTaggedLines(String startTag, String endTag) {
    return allBetweenTaggedLines(startTag, endTag, false);
  }

  public TaggableText allBetweenTaggedLines(String startTag, String endTag, boolean useRegex) {
    var scanner = new TextScanner(text);
    this.text = useRegex
        ? scanner.getAllBetweenLinesMatching(startTag, endTag)
        : scanner.getAllBetweenLinesContaining(startTag, endTag);
    return this;
  }

  @Override
  public String toString() {
    return text;
  }
}

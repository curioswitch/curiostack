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
/*
 * Copyright (c) 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.curioswitch.common.testing.assertj.proto;

/**
 * Whether or not a sub-message tree is considered part of the enclosing scope.
 *
 * <p>This enables {@link FieldScopeLogic}s and the {@code ProtoTruthMessageDifferencer} to work
 * together on traversing a message, instead of either class doing redundant work. The need for
 * {@code NONRECURSIVE} arises from sub-messages. For example:
 *
 * <p><code>
 *   message Foo {
 *     optional Bar bar = 1;
 *   }
 *
 *   message Bar {
 *     optional Baz baz = 1;
 *   }
 *
 *   message Baz {
 *     optional string name = 1;
 *     optional int64 id = 2;
 *   }
 * </code>
 *
 * <p>A {@link FieldScopeLogic} which excludes everything except 'Baz.name', when asked if 'Foo.bar'
 * should be ignored, cannot know whether it should be excluded or not without scanning all of
 * 'Foo.bar' for Baz submessages, and whether they have the name field set. We could scan the entire
 * message to make this decision, but the message differencer will be scanning anyway if we choose
 * not to excluded it, which creates redundant work. {@code NONRECURSIVE} is the solution to this
 * problem: The logic defers the decision back to the message differencer, which proceeds with the
 * complete scan of 'Foo.bar', and excludes the entire submessage if and only if nothing in
 * 'Foo.bar' was determined to be un-excludable.
 */
enum FieldScopeResult {
  /** This field is included in this scope, but children might be excludable. */
  INCLUDED_NONRECURSIVELY(true, false),
  /** This field and all its children are included in the scope. */
  INCLUDED_RECURSIVELY(true, true),
  /** This field is excluded from the scope, but children might be includable. */
  EXCLUDED_NONRECURSIVELY(false, false),
  /** This field and all its children are excluded from the scope. */
  EXCLUDED_RECURSIVELY(false, true);

  public static FieldScopeResult of(boolean included, boolean recursively) {
    if (included) {
      return recursively ? INCLUDED_RECURSIVELY : INCLUDED_NONRECURSIVELY;
    } else {
      return recursively ? EXCLUDED_RECURSIVELY : EXCLUDED_NONRECURSIVELY;
    }
  }

  private final boolean included;
  private final boolean recursive;

  FieldScopeResult(boolean included, boolean recursive) {
    this.included = included;
    this.recursive = recursive;
  }

  /** Whether this field should be included or not. */
  boolean included() {
    return included;
  }

  /**
   * Whether this field's sub-children should also be unilaterally included or excluded, conditional
   * on {@link #included()}
   */
  boolean recursive() {
    return recursive;
  }
}

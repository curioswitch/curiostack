/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

// Includes work from:
/*
 * Copyright (c) 2016 Google, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.curioswitch.common.testing.assertj.proto.ProtoAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test to ensure that Truth.assertThat and ProtoTruth.assertThat can coexist while statically
 * imported. The tests themselves are simple and dumb, as what's really being tested here is whether
 * or not this file compiles.
 */
@RunWith(JUnit4.class)
public class OverloadResolutionTest extends ProtoAssertTestBase {
  public OverloadResolutionTest() {
    // We don't bother testing Proto3 because it's immaterial to this test, and we want to ensure
    // that using Iterable<MyMessage> works, not just Iterable<Message>.
    super(TestType.IMMUTABLE_PROTO2);
  }

  @Override
  protected TestMessage2 parse(String string) {
    return (TestMessage2) super.parse(string);
  }

  @Test
  public void testObjectOverloads_testMessages_normalMethods() {
    TestMessage2 message = parse("r_string: \"foo\" r_string: \"bar\"");
    TestMessage2 eqMessage = parse("r_string: \"foo\" r_string: \"bar\"");
    TestMessage2 diffMessage = parse("r_string: \"bar\" r_string: \"foo\"");
    Object object = message;
    Object eqObject = eqMessage;
    Object diffObject = diffMessage;

    assertThat(message).isSameAs(object);
    assertThat(message).isNotSameAs(eqMessage);
    assertThat(message).isEqualTo(eqMessage);
    assertThat(message).isNotEqualTo(diffMessage);
    assertThat(message).isEqualTo(eqObject);
    assertThat(message).isNotEqualTo(diffObject);
  }

  @Test
  public void testObjectOverloads_testMessages_specializedMethods() {
    TestMessage2 message = parse("r_string: \"foo\" r_string: \"bar\"");
    TestMessage2 diffMessage = parse("r_string: \"bar\" r_string: \"foo\"");

    assertThat(message).ignoringRepeatedFieldOrder().isEqualTo(diffMessage);
  }

  @Test
  public void testObjectOverloads_objects_actuallyMessages() {
    TestMessage2 message = parse("r_string: \"foo\" r_string: \"bar\"");
    TestMessage2 eqMessage = parse("r_string: \"foo\" r_string: \"bar\"");
    TestMessage2 diffMessage = parse("r_string: \"bar\" r_string: \"foo\"");
    Object object = message;
    Object eqObject = eqMessage;
    Object diffObject = diffMessage;

    assertThat(object).isSameAs(message);
    assertThat(object).isNotSameAs(eqObject);
    assertThat(object).isEqualTo(eqObject);
    assertThat(object).isNotEqualTo(diffObject);
    assertThat(object).isEqualTo(eqMessage);
    assertThat(object).isNotEqualTo(diffMessage);
  }

  @Test
  public void testObjectOverloads_objects_actuallyNotMessages() {
    TestMessage2 message = parse("r_string: \"foo\" r_string: \"bar\"");
    Object altObject = 1111;
    Object eqAltObject = (1 + 10 + 100 + 1000);

    assertThat(altObject).isEqualTo(eqAltObject);
    assertThat(altObject).isNotEqualTo(message);
  }
}

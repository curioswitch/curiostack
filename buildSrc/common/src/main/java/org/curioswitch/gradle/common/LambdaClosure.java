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

package org.curioswitch.gradle.common;

import groovy.lang.Closure;

public final class LambdaClosure {

  public static <T> Closure<Void> of(OneArgClosureFunction<T> function) {
    return new OneArgClosure<>(function);
  }

  @FunctionalInterface
  public interface OneArgClosureFunction<T> {
    void call(T arg);
  }

  private static class OneArgClosure<T> extends Closure<Void> {

    private final OneArgClosureFunction<T> function;

    public OneArgClosure(OneArgClosureFunction<T> function) {
      super(function); // null doesn't work, but anything else is fine as it's not used.
      this.function = function;
    }

    protected Object doCall(Object arguments) {
      @SuppressWarnings("unchecked")
      T arg = (T) arguments;
      function.call(arg);
      return null;
    }
  }

  private LambdaClosure() {}
}

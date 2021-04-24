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

package org.curioswitch.common.protobuf.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription.ForLoadedType;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackManipulation.Compound;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.curioswitch.common.protobuf.json.LocalVariables.VariableHandle;

/** A container of the local variables of a given method. */
final class LocalVariables<T extends VariableHandle> {

  /**
   * A handle for accessing a variable. Methods should generally create an {@link Enum} that
   * implements this interface for type-safe accessing of variables. If the local variables are
   * dynamically computed, a non-{@link Enum} is fine too as long as it is can be used as a {@link
   * Map} key.
   */
  interface VariableHandle {
    String name();
  }

  /**
   * Returns a {@link Builder} of {@link LocalVariables} which computes {@link VariableAccessor}s
   * for all the parameters in {@code method}. All parameter names in {@code method} must have a
   * corresponding {@link VariableHandle} in {@code handles}.
   */
  static <T extends VariableHandle> Builder<T> builderForMethod(
      MethodDescription method, T[] handles) {
    return new Builder<>(method, handles);
  }

  /**
   * A {@link Builder} for adding local variable definitions when constructing {@link
   * LocalVariables}.
   */
  static class Builder<T extends VariableHandle> {
    private final LinkedHashMap<T, VariableAccessor> accessors;
    private final List<Class<?>> frameLocalTypes;

    private Builder(MethodDescription method, T[] handles) {
      accessors = new LinkedHashMap<>();
      frameLocalTypes = new ArrayList<>();
      Map<String, T> handlesByName = new HashMap<>();
      for (T handle : handles) {
        String name = handle.name();
        if (handlesByName.containsKey(name)) {
          throw new IllegalArgumentException(
              "Duplicate VariableHandle with same name, there must only be one handle per variable "
                  + "name: "
                  + name);
        }
        handlesByName.put(name, handle);
      }
      for (ParameterDescription param : method.getParameters()) {
        T handle = handlesByName.get(param.getName());
        if (handle == null) {
          throw new IllegalStateException(
              "Could not find VariableHandle with same variableName as parameter. Make sure variable "
                  + "handles match names of method parameters. param name: "
                  + param.getName());
        }
        accessors.put(
            handle,
            new VariableAccessor(
                MethodVariableAccess.of(param.getType()), param.getOffset(), param.getType()));
      }
    }

    /** Add a local variable with the specified {@code type}, referenceable by {@code handle}. */
    Builder<T> add(Class<?> type, T handle) {
      int nextOffset = /* this */ 1 + accessors.size();
      TypeDefinition def = new ForLoadedType(type);
      accessors.put(handle, new VariableAccessor(MethodVariableAccess.of(def), nextOffset, def));
      frameLocalTypes.add(type);
      return this;
    }

    /**
     * Returns the built {@link LocalVariables}. No local variables can be added to a method after
     * this.
     */
    LocalVariables<T> build() {
      return new LocalVariables<>(accessors, frameLocalTypes);
    }
  }

  private final Map<T, VariableAccessor> accessors;
  private final List<Class<?>> frameLocalTypes;

  private LocalVariables(Map<T, VariableAccessor> accessors, List<Class<?>> frameLocalTypes) {
    this.accessors = new LinkedHashMap<>(accessors);
    this.frameLocalTypes = frameLocalTypes;
  }

  /**
   * Returns a {@link StackManipulation} that will initialize all non-method parameter local
   * variables to their default values. Must be called at the beginning of any method
   * implementation.
   */
  StackManipulation initialize() {
    int numMethodParams = accessors.size() - frameLocalTypes.size();
    List<StackManipulation> ops = new ArrayList<>();
    accessors.values().stream().skip(numMethodParams).forEach(var -> ops.add(var.initialize()));
    return new Compound(ops);
  }

  /** Returns a {@link StackManipulation} that will load {@code var} onto the execution stack. */
  StackManipulation load(T var) {
    return accessors.get(var).load();
  }

  /**
   * Returns a {@link StackManipulation} that will store the top item on the execution stack into
   * {@code var}.
   */
  StackManipulation store(T var) {
    return accessors.get(var).store();
  }

  /**
   * Returns the total stack space needed by all local variables, including the reference to this.
   * The result can be used as is when determining the stack size of a method.
   */
  int stackSize() {
    return /* this */ 1 + accessors.values().size();
  }

  private static final class VariableAccessor {
    private final MethodVariableAccess access;
    private final int offset;
    private final TypeDefinition type;

    private VariableAccessor(MethodVariableAccess access, int offset, TypeDefinition type) {
      this.access = access;
      this.offset = offset;
      this.type = type;
    }

    private StackManipulation initialize() {
      return new Compound(DefaultValue.of(type), store());
    }

    private StackManipulation load() {
      return access.loadFrom(offset);
    }

    private StackManipulation store() {
      return access.storeAt(offset);
    }
  }
}

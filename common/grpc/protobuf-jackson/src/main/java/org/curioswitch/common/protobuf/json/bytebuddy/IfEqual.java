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
package org.curioswitch.common.protobuf.json.bytebuddy;

import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

/**
 * A {@link StackManipulation} which jumps to a destination if two items on the execution stack are
 * equal. For reference types, there should only be a single boolean on the execution stack which is
 * the result of {@link Object#equals(Object)}.
 *
 * <p>Used for if-statements like
 *
 * <pre>{code
 *   if (a != b) {
 *     ...
 *   }
 *   // destination
 * }</pre>
 */
public final class IfEqual implements StackManipulation {

  private final Class<?> variableType;
  private final Label destination;

  public IfEqual(Class<?> variableType, Label destination) {
    this.variableType = variableType;
    this.destination = destination;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
    final int opcode;
    Size size = new Size(-StackSize.of(variableType).getSize() * 2, 0);
    if (variableType == int.class || variableType == boolean.class) {
      opcode = Opcodes.IF_ICMPEQ;
    } else if (variableType == long.class) {
      methodVisitor.visitInsn(Opcodes.LCMP);
      opcode = Opcodes.IFEQ;
    } else if (variableType == float.class) {
      methodVisitor.visitInsn(Opcodes.FCMPG);
      opcode = Opcodes.IFEQ;
    } else if (variableType == double.class) {
      methodVisitor.visitInsn(Opcodes.DCMPG);
      opcode = Opcodes.IFEQ;
    } else {
      // Reference type comparison assumes the result of Object.equals is already on the stack.
      opcode = Opcodes.IFNE;
      // There is only a boolean on the stack, so we only consume one item, unlike the others that
      // consume both.
      size = new Size(-1, 0);
    }
    methodVisitor.visitJumpInsn(opcode, destination);
    return size;
  }
}

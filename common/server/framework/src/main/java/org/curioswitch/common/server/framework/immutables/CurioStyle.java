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
package org.curioswitch.common.server.framework.immutables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

/**
 * {@link Style} which applies curio conventions to {@link org.immutables.value.Value.Immutable}
 * objects. It is recommended that all {@link org.immutables.value.Value.Immutable} types use this
 * style.
 *
 * <p>Default methods in interfaces will be recognized and all implementation will be
 * package-private. The {@link org.immutables.value.Value.Immutable} must expose a subclass of the
 * generated builder, e.g.,
 *
 * <pre>{@code
 * {@literal @}CurioStyle
 * {@literal @}Immutable
 * public interface MyObject {
 *   class Builder extends ImmutableMyObject.Builder {}
 *
 *   String foo();
 *
 *   boolean bar();
 * }
 *
 * }</pre>
 */
@Target(ElementType.TYPE)
@Style(
  deepImmutablesDetection = true,
  defaultAsDefault = true,
  builderVisibility = BuilderVisibility.PACKAGE,
  visibility = ImplementationVisibility.PACKAGE
)
public @interface CurioStyle {}

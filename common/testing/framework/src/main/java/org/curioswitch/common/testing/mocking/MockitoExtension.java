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
package org.curioswitch.common.testing.mocking;

import static org.mockito.Mockito.mock;

import com.google.auto.service.AutoService;
import java.lang.reflect.Parameter;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * TODO(choko): Remove once mockito adds official junit5 support.
 *
 * <p>{@code MockitoExtension} showcases the {@link TestInstancePostProcessor} and {@link
 * ParameterResolver} extension APIs of JUnit 5 by providing dependency injection support at the
 * field level and at the method parameter level via Mockito 2.x's {@link Mock @Mock} annotation.
 *
 * @since 5.0
 */
@AutoService(Extension.class)
public class MockitoExtension
    implements BeforeEachCallback, TestInstancePostProcessor, ParameterResolver {

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    MockitoAnnotations.initMocks(testInstance);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().isAnnotationPresent(Mock.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return getMock(parameterContext.getParameter(), extensionContext);
  }

  private static Object getMock(Parameter parameter, ExtensionContext extensionContext) {
    Class<?> mockType = parameter.getType();
    Store mocks = extensionContext.getStore(Namespace.create(MockitoExtension.class, mockType));
    String mockName = getMockName(parameter);

    if (mockName != null) {
      return mocks.getOrComputeIfAbsent(mockName, key -> mock(mockType, mockName));
    } else {
      return mocks.getOrComputeIfAbsent(mockType.getCanonicalName(), key -> mock(mockType));
    }
  }

  @Nullable
  private static String getMockName(Parameter parameter) {
    String explicitMockName = parameter.getAnnotation(Mock.class).name().trim();
    if (!explicitMockName.isEmpty()) {
      return explicitMockName;
    } else if (parameter.isNamePresent()) {
      return parameter.getName();
    }
    return null;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    MockitoAnnotations.initMocks(context.getRequiredTestInstance());
    while (context.getParent().isPresent()
        && context.getParent().get().getTestInstance().isPresent()) {
      context = context.getParent().get();
      MockitoAnnotations.initMocks(context.getRequiredTestInstance());
    }
  }
}

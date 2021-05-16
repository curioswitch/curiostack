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

package org.curioswitch.common.server.framework.logging;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.TriConsumer;

public class RequestLoggingContextInjector implements ContextDataInjector {

  @Override
  public StringMap injectContextData(List<Property> properties, StringMap reusable) {
    ImmutableMap<String, String> current = RequestLoggingContext.get();
    if (properties == null || properties.isEmpty()) {
      return new JdkMapAdapaterStringMap(current);
    }
    ImmutableMap.Builder<String, String> injected =
        ImmutableMap.builderWithExpectedSize(current.size() + properties.size());
    injected.putAll(current);
    for (Property prop : properties) {
      // This will throw if a property name is already in the context, don't worry about it for now.
      injected.put(prop.getName(), prop.getValue());
    }
    return new JdkMapAdapaterStringMap(injected.build());
  }

  @Override
  public ReadOnlyStringMap rawContextData() {
    return new JdkMapAdapaterStringMap(RequestLoggingContext.get());
  }

  private static class JdkMapAdapaterStringMap implements StringMap {

    private final Map<String, String> map;

    private JdkMapAdapaterStringMap(Map<String, String> map) {
      this.map = map;
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void freeze() {}

    @Override
    public boolean isFrozen() {
      return true;
    }

    @Override
    public void putAll(ReadOnlyStringMap source) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putValue(String key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void remove(String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> toMap() {
      return map;
    }

    @Override
    public boolean containsKey(String key) {
      return map.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> void forEach(BiConsumer<String, ? super V> action) {
      map.forEach((key, value) -> action.accept(key, (V) value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V, S> void forEach(TriConsumer<String, ? super V, S> action, S state) {
      map.forEach((key, value) -> action.accept(key, (V) value, state));
    }

    @Override
    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    public <V> V getValue(String key) {
      return (V) map.get(key);
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public int size() {
      return map.size();
    }
  }
}

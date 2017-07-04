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

package org.curioswitch.common.server.framework.auth.ssl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonFileSslCommonNamesProvider implements SslCommonNamesProvider {

  private static final Logger logger = LogManager.getLogger();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private volatile Set<String> names = ImmutableSet.of();

  public void processFile(Path path) {
    final Map<String, ?> rpcAcls;
    try {
      @SuppressWarnings("unchecked")
      Map<String, ?> localRpcAcls = (Map<String, ?>) OBJECT_MAPPER.readValue(path.toFile(), Map.class);
      rpcAcls = localRpcAcls;
    } catch (IOException e) {
      logger.warn("Error parsing rpcacls file.", e);
      return;
    }
    names = ImmutableSet.copyOf(rpcAcls.keySet());
  }

  @Override
  public Set<String> get() {
    return names;
  }
}

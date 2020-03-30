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
package org.curioswitch.common.server.framework.armeria;

import com.google.common.io.ByteStreams;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.function.BiConsumer;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemObject;
import org.curioswitch.common.server.framework.crypto.KeyUtil;

/**
 * A very hacky way of using keys with {@link SslContextBuilder}, transparently converting to PKCS#8
 * as needed. Not for general use.
 */
public final class SslContextKeyConverter {

  public static void execute(
      InputStream keyCertChainFile,
      InputStream keyFile,
      BiConsumer<InputStream, InputStream> operation) {
    final byte[] key;
    final byte[] keyCertChain;
    try {
      key = ByteStreams.toByteArray(keyFile);
      keyCertChain = ByteStreams.toByteArray(keyCertChainFile);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read file to bytes.", e);
    }

    try {
      operation.accept(new ByteArrayInputStream(keyCertChain), new ByteArrayInputStream(key));
    } catch (Exception e) {
      // Try to convert the key to PCKS8.
      PrivateKey privateKey = KeyUtil.loadPrivateKey(key);
      final PemObject encoded;
      try {
        JcaPKCS8Generator generator = new JcaPKCS8Generator(privateKey, null);
        encoded = generator.generate();
      } catch (PemGenerationException ex) {
        throw new IllegalStateException("Could not generate PKCS8", ex);
      }

      StringWriter sw = new StringWriter();
      try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
        pw.writeObject(encoded);
      } catch (IOException ex) {
        throw new UncheckedIOException("Could not write key to String, can't happen.", ex);
      }
      byte[] pkcs8key = sw.toString().getBytes(StandardCharsets.UTF_8);
      operation.accept(new ByteArrayInputStream(keyCertChain), new ByteArrayInputStream(pkcs8key));
    }
  }

  private SslContextKeyConverter() {}
}

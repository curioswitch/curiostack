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

package org.curioswitch.common.server.framework.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public final class KeyUtil {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static PrivateKey loadPrivateKey(byte[] encodedKey) {
    try (PEMParser parser = newParser(encodedKey)) {
      Object obj;
      while ((obj = parser.readObject()) != null) {
        if (obj instanceof PEMKeyPair) {
          return newConverter().getKeyPair((PEMKeyPair) obj).getPrivate();
        } else if (obj instanceof PrivateKeyInfo) {
          return newConverter().getPrivateKey((PrivateKeyInfo) obj);
        }
      }
      throw new IllegalStateException("Could not find private key.");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not load private key.", e);
    }
  }

  public static PublicKey loadPublicKey(byte[] encodedKey) {
    try (PEMParser parser = newParser(encodedKey)) {
      Object obj;
      while ((obj = parser.readObject()) != null) {
        if (obj instanceof SubjectPublicKeyInfo) {
          return newConverter().getPublicKey((SubjectPublicKeyInfo) obj);
        }
      }
      throw new IllegalStateException("Could not find public key.");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not load public key.", e);
    }
  }

  private static JcaPEMKeyConverter newConverter() {
    return new JcaPEMKeyConverter().setProvider("BC");
  }

  private static PEMParser newParser(byte[] encodedKey) {
    return new PEMParser(
        new InputStreamReader(new ByteArrayInputStream(encodedKey), StandardCharsets.UTF_8));
  }

  private KeyUtil() {}
}

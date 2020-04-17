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
package org.curioswitch.common.server.framework.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import javax.inject.Inject;

/** A signer of data using the 256-bit elliptic curve DSA algorithm. */
public class ECDSA256Signer {

  private final PrivateKey privateKey;

  @Inject
  public ECDSA256Signer(SignerConfig config) {
    privateKey = KeyUtil.loadPrivateKey(Base64.getDecoder().decode(config.getPrivateKeyBase64()));
  }

  /** Returns the signature for {@code payload}. */
  public byte[] sign(byte[] payload) {
    final Signature signature;
    try {
      signature = Signature.getInstance("SHA256withECDSA");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(
          "Could not load ECDSA algorithm. Is bouncycastle on the classpath?");
    }
    try {
      signature.initSign(privateKey);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException("Private key not an ECDSA key.");
    }
    try {
      signature.update(payload);
      return signature.sign();
    } catch (SignatureException e) {
      throw new IllegalStateException("Could not sign payload.", e);
    }
  }
}

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

package org.curioswitch.common.server.framework.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** A verifier of ECDSA256 signatures. */
public class ECDSA256Verifier {

  private static final Logger logger = LogManager.getLogger();

  private final PublicKey publicKey;

  @Inject
  public ECDSA256Verifier(SignerConfig config) {
    publicKey = KeyUtil.loadPublicKey(Base64.getDecoder().decode(config.getPublicKeyBase64()));
  }

  /** Verifies that {@code payloadSignature} is correct for {@code payload}. */
  public boolean verify(byte[] payload, byte[] payloadSignature) {
    final Signature signature;
    try {
      signature = Signature.getInstance("SHA256withECDSA");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(
          "Could not load ECDSA algorithm. Is bouncycastle on the classpath?");
    }
    try {
      signature.initVerify(publicKey);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException("Public key not an ECDSA key.", e);
    }
    try {
      signature.update(payload);
      return signature.verify(payloadSignature);
    } catch (SignatureException e) {
      logger.warn("Invalid signature.", e);
      return false;
    }
  }
}

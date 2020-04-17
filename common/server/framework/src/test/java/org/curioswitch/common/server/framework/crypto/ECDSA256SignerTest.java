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

import static org.curioswitch.common.testing.assertj.CurioAssertions.assertThat;

import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;

public class ECDSA256SignerTest {

  private ECDSA256Signer signer;
  private ECDSA256Verifier verifier;

  @Before
  public void setUp() throws Exception {
    SignerConfig config =
        ImmutableSignerConfig.builder()
            .privateKeyBase64(
                Base64.getEncoder()
                    .encodeToString(
                        Resources.toByteArray(Resources.getResource("test-signer-private.pem"))))
            .publicKeyBase64(
                Base64.getEncoder()
                    .encodeToString(
                        Resources.toByteArray(Resources.getResource("test-signer-public.pem"))))
            .build();
    signer = new ECDSA256Signer(config);
    verifier = new ECDSA256Verifier(config);
  }

  @Test
  public void signedCanBeVerified() {
    byte[] payload = "highlysensitiveinformation".getBytes(StandardCharsets.UTF_8);
    byte[] signature = signer.sign(payload);
    assertThat(verifier.verify(payload, signature)).isTrue();
  }
}

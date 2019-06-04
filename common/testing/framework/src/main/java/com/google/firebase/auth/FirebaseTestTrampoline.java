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
package com.google.firebase.auth;

import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.json.JsonFactory;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Provides access to package-private methods of {@link com.google.firebase.auth} for use in tests.
 * Trampolines are more type-safe than using reflection. Users should use the methods in {@link
 * org.curioswitch.common.testing.auth.firebase.FirebaseTestUtil} instead of calling these directly.
 */
public class FirebaseTestTrampoline {

  public static FirebaseToken parseToken(JsonFactory jsonFactory, String tokenString) {
    try {
      return new FirebaseToken(IdToken.parse(jsonFactory, tokenString).getPayload());
    } catch (IOException e) {
      throw new UncheckedIOException("Could not parse firebase token.", e);
    }
  }

  private FirebaseTestTrampoline() {}
}

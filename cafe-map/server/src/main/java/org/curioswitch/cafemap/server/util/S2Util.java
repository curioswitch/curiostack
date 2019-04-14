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

package org.curioswitch.cafemap.server.util;

import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;
import org.curioswitch.cafemap.api.LatLng;
import org.curioswitch.cafemap.api.LatLngBounds;

public final class S2Util {

  public static S2LatLng convertFromLatLng(LatLng latLng) {
    return S2LatLng.fromDegrees(latLng.getLatitude(), latLng.getLongitude());
  }

  public static S2LatLngRect convertFromLatLngBounds(LatLngBounds latLngBounds) {
    return new S2LatLngRect(
        convertFromLatLng(latLngBounds.getSouthWest()),
        convertFromLatLng(latLngBounds.getNorthEast()));
  }

  private S2Util() {}
}

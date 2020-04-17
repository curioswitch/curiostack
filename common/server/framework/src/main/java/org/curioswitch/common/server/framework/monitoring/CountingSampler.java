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
package org.curioswitch.common.server.framework.monitoring;

import brave.sampler.Sampler;
import com.google.common.math.IntMath;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Sampler} which samples traces at a given sample rate. Unlike the upstream sampler, this
 * supports smaller sampling rates down to 0.000001 and does not use synchronization. The smaller
 * the sampling rate, the more memory will be used, with a maximum of 128K (1 million bits).
 */
class CountingSampler extends Sampler {

  /**
   * @param rate 0 means never sample, 1 means always sample. Otherwise minimum sample rate is 0.01,
   *     or 1% of traces
   */
  public static Sampler create(final float rate) {
    if (rate == 0) return NEVER_SAMPLE;
    if (rate == 1.0) return ALWAYS_SAMPLE;
    if (rate < 0.000001f || rate > 1) {
      throw new IllegalArgumentException("rate should be between 0.000001f and 1: was " + rate);
    }
    return new CountingSampler(rate);
  }

  private final AtomicInteger counter;
  private final int numBuckets;
  private final BitSet sampleDecisions;

  CountingSampler(float samplingRate) {
    counter = new AtomicInteger();

    numBuckets = numBuckets(samplingRate);
    int numFilledBuckets = (int) (samplingRate * numBuckets);

    sampleDecisions = randomBitSet(numBuckets, numFilledBuckets, new Random());
  }

  @Override
  public boolean isSampled(long traceId) {
    // It's highly unlikely the total number of sampling decisions would reach the limit of a Long,
    // but if it did it would wrap to a negative number but the mod will remain positive and
    // will continue to cycle through decisions.
    return sampleDecisions.get(IntMath.mod(counter.getAndIncrement(), numBuckets));
  }

  private static int numBuckets(float samplingRate) {
    if (samplingRate >= 0.01) {
      return 100;
    } else if (samplingRate >= 0.001) {
      return 1000;
    } else if (samplingRate >= 0.0001) {
      return 10000;
    } else if (samplingRate >= 0.00001) {
      return 100000;
    } else {
      return 1000000;
    }
  }

  /**
   * Reservoir sampling algorithm borrowed from Stack Overflow.
   *
   * <p>http://stackoverflow.com/questions/12817946/generate-a-random-bitset-with-n-1s
   */
  private static BitSet randomBitSet(int size, int cardinality, Random rnd) {
    BitSet result = new BitSet(size);
    int[] chosen = new int[cardinality];
    int i;
    for (i = 0; i < cardinality; ++i) {
      chosen[i] = i;
      result.set(i);
    }
    for (; i < size; ++i) {
      int j = rnd.nextInt(i + 1);
      if (j < cardinality) {
        result.clear(chosen[j]);
        result.set(i);
        chosen[j] = i;
      }
    }
    return result;
  }
}

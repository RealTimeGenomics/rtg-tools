/*
 * Copyright (c) 2016. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.rtg.simulation.variants;

import com.rtg.util.PortableRandom;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Generate a single homozygous mutation on a template using a specification. The specification consists of
 * a string with the following grammar:
 * <pre>
 * single ::= {[count]{"X", "I", "J", "D", "=", "E"}}+
 *
 * The codes specify:
 * X mutate to a valid nucleotide not equal to the template but may be equal to a previous mutation.
 * Y mutate to a valid nucleotide not equal to the template nor equal to a previous mutation.
 * I insert a random valid nucleotide into the mutated result.
 * J insert a random valid nucleotide into the mutated result not equal to a previous mutation at that point in the mutated result.
 * D delete a nucleotide from the template.
 * = or E use the same nucleotide as the template.
 * </pre>
 * The class is re-entrant and thread safe.
 */
class MutatorSingle extends IntegralAbstract {

  private final String mSpecification;

  private final int mRefLength;
  private final int mMutLength;

  /**
   * Expand a spec with counts in it e.g. <code>5I</code> becomes <code>IIIII</code>
   * @param spec containing counts e.g. <code>5I</code>
   * @return a spec with the counted mutations expanded
   */
  static String expandSpec(String spec) {
    final StringBuilder sb = new StringBuilder();
    int count = 0;
    for (int s = 0; s < spec.length(); ++s) {
      final char c = spec.charAt(s);
      if (c >= '0' && c <= '9') {
        count = count * 10 + c - '0';
      } else {
        if (count == 0) {
          count = 1;
        }
        for (int i = 0; i < count; ++i) {
          sb.append(c);
        }
        count = 0;
      }
    }
    if (count != 0) {
      throw new RuntimeException(spec);
    }
    return sb.toString();
  }

  /**
   * @param specification for the mutation. See class documentation.
   */
  MutatorSingle(String specification) {
    mSpecification = expandSpec(specification);
    int t = 0;
    int r = 0;
    for (int s = 0; s < mSpecification.length(); ++s) {
      final char c = mSpecification.charAt(s);
      switch (c) {
        case 'X':
        case 'Y':
        case '=':
        case 'E':
          ++t;
          ++r;
          break;
        case 'I':
        case 'J':
          ++r;
          break;
        case 'D':
          ++t;
          break;
        default:
          throw new RuntimeException(mSpecification);
      }
    }
    mRefLength = t;
    mMutLength = r;
  }

  /**
   * @return length of mutation on reference
   */
  public int getReferenceLength() {
    return mRefLength;
  }

  /**
   * @return length of mutated result
   */
  public int getMutationLength() {
    return mMutLength;
  }

  /**
   * Almost but not quite the same as the method in the Mutator interface. The difference is that it checks another
   * result to make sure the mutation is different (for the case when this is the second of a heterozygous pair).
   * @param template the template to mutate
   * @param position the zero based position in the template to generate a mutation
   * @param random number generator used for generating mutations.
   * @param other a previous mutation - a check is made that an X code differs from this - will be ignored if it is null.
   * @return the generated mutation
   */
  MutatorResult generateMutation(byte[] template, int position, PortableRandom random, byte[] other) {
    //guaranteed to be long enough to hold the result
    //doing this here rather than as a field ensures the class is re-entrant.
    final byte[] result = new byte[mSpecification.length()];
    int t = position;
    int r = 0;
    for (int s = 0; s < mSpecification.length(); ++s) {
      assert 0 <= r && r <= result.length;
      assert position <= t && t <= template.length;
      final char c = mSpecification.charAt(s);
      switch (c) {
        case 'X':
          if (t >= template.length) {
            return null;
          }
          result[r] = minus(random, template[t]);
          ++t;
          ++r;
          break;
        case 'Y':
          if (t >= template.length) {
            return null;
          }
          final byte tem = template[t];
          if (other == null) {
            result[r] = minus(random, tem);
          } else {
            result[r] = minus(random, tem, other[r]);
          }
          ++t;
          ++r;
          break;
        case 'I':
          result[r] = random(random);
          ++r;
          break;
        case 'J':
          if (other == null) {
            result[r] = random(random);
          } else {
            result[r] = minus(random, other[r]);
          }
          ++r;
          break;
        case 'D':
          ++t;
          break;
        case '=':
        case 'E':
          if (t >= template.length) {
            return null;
          }
          result[r] = template[t];
          ++t;
          ++r;
          break;
        default:
          throw new RuntimeException(mSpecification);
      }
    }
    final byte[] res = new byte[r];
    System.arraycopy(result, 0, res, 0, r);
    return new MutatorResult(res, res, t - position);
  }

  static byte minus(PortableRandom random, byte a, byte b) {
    if (a == b) {
      return minus(random, a);
    }
    if (a > b) {
      return minus(random, b, a);
    }
    final int ra = random.nextInt(2) + 1;
    final int sa = ra + (ra >= a ? 1 : 0);
    final int ta = sa + (sa >= b ? 1 : 0);
    return (byte) ta;
  }

  static byte minus(PortableRandom random, byte a) {
    final int ra = random.nextInt(3) + 1;
    final int sa = ra + (ra >= a ? 1 : 0);
    return (byte) sa;
  }

  static byte random(PortableRandom random) {
    return (byte) (random.nextInt(4) + 1);
  }

  @Override
  public boolean integrity() {
    Exam.assertNotNull(mSpecification);
    return true;
  }

  @Override
  public void toString(StringBuilder sb) {
    sb.append(mSpecification);
  }

}

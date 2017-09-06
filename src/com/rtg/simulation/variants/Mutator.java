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
 * Mutate a template using a specification. The specification consists of
 * a string with the following grammar:
 * <pre>
 * single ::= {"X", "Y", "J", "I", "D", "=", "E"}+
 * specification ::= single [{":", "_" } single]
 *
 * The two halves of the specification apply to the two haplotypes (if there is only one then the
 * mutation is homozygous). The codes specify:
 * X mutate to a valid nucleotide not equal to the template but may be equal to a previous mutation.
 * Y mutate to a valid nucleotide not equal to the template nor equal to a previous mutation.
 * I insert a random valid nucleotide into the mutated result.
 * J insert a random valid nucleotide into the mutated result not equal to a previous mutation at that point in the mutated result.
 * D delete a nucleotide from the template.
 * = or E use the same nucleotide as the template.
 * </pre>
 */
public class Mutator extends IntegralAbstract {

  private final MutatorSingle mFirst;

  private final MutatorSingle mSecond;

  /**
   * @param specification single class docs
   */
  public Mutator(final String specification) {
    final String[] split = specification.split("_|:");
    switch (split.length) {
      case 1:
        mFirst = new MutatorSingle(split[0]);
        mSecond = null;
        break;
      case 2:
        int i = 0;
        for (; i < split[0].length() && i < split[1].length(); ++i) {
          if (split[0].charAt(split[0].length() - 1 - i) == 'E' && split[1].charAt(split[1].length() - 1 - i) == 'E') {
            continue;
          }
          break;
        }
        mFirst = new MutatorSingle(split[0].substring(0, split[0].length() - i));
        mSecond = new MutatorSingle(split[1].substring(0, split[1].length() - i));
        break;
      default:
        throw new RuntimeException(specification);
    }
    assert integrity();
  }

  /**
   * @return length of reference that is mutated
   */
  public int getReferenceLength() {
    assert mSecond == null || mSecond.getReferenceLength() == mFirst.getReferenceLength();
    return mFirst.getReferenceLength();
  }

  /**
   * @return true if mutation contains an insertion or deletion
   */
  public boolean isIndel() {
    return mFirst.getReferenceLength() != mFirst.getMutationLength() || (mSecond != null && mSecond.getReferenceLength() != mSecond.getMutationLength());
  }

  /**
   * Generates a mutation for the specified template.
   * @param template the template to mutate
   * @param position the zero based position in the template to generate a mutation
   * @param random number generator used for generating mutations.
   * @return the generated mutation (may be null if a valid mutation cannot be generated at that position).
   */
  public MutatorResult generateMutation(byte[] template, int position, PortableRandom random) {
    final MutatorResult first = mFirst.generateMutation(template, position, random, null);
    final byte[] firstHaplotype = first.getFirstHaplotype();
    if (mSecond == null) {
      return new MutatorResult(firstHaplotype, firstHaplotype, first.getConsumed());
    }
    final MutatorResult second = mSecond.generateMutation(template, position, random, firstHaplotype);
    assert first.getConsumed() == second.getConsumed();
    return new MutatorResult(firstHaplotype, second.getFirstHaplotype(), first.getConsumed());
  }

  @Override
  public void toString(StringBuilder sb) {
    final String right = mSecond != null ? ":" + mSecond : "";
    sb.append("Mutator:").append(mFirst).append(right);
  }

  @Override
  public final boolean integrity() {
    Exam.assertNotNull(mFirst);
    return true;
  }

}

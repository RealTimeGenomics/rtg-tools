/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.alignment;

import com.rtg.mode.DNA;
import com.rtg.mode.DnaUtils;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;

import junit.framework.TestCase;


/**
 */
public class ActionsValidatorTest extends TestCase {

  public void testNull() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    assertFalse(av.isValid(null, null, 0, null, Integer.MAX_VALUE));
    final String error = av.getErrorDetails(null, null, 0, null, 0);
    assertEquals("ValidatingEditDistance action problem: actions array is null" + StringUtils.LS, error);
  }

  public void testTooShort() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    assertFalse(av.isValid(new int[2], null, 0, null, Integer.MAX_VALUE));
    final String error = av.getErrorDetails(new int[2], null, 0, null, 0);
    assertEquals("ValidatingEditDistance action problem: actions array is too short: 2" + StringUtils.LS, error);
  }

  public void testNumberOfActions() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final int[] actions = new int[3];
    actions[ActionsHelper.ACTIONS_LENGTH_INDEX] = 4;
    assertFalse(av.isValid(actions, null, 0, null, Integer.MAX_VALUE));
    final String error = av.getErrorDetails(actions, null, 0, null, 0);
    assertEquals("ValidatingEditDistance action problem: int[] is shorter than number of actions requires" + StringUtils.LS, error);
  }

  public void testMaxIntAction() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final int[] actions = new int[3];
    actions[ActionsHelper.ALIGNMENT_SCORE_INDEX] = Integer.MAX_VALUE;
    assertTrue(av.isValid(actions, null, 0, null, 0));
  }

  public void testSameFail() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("aata");
    final int[] actions = ActionsHelper.build("====", 0, 0);
    assertFalse(av.isValid(actions, read, read.length, tmpl, Integer.MAX_VALUE));
    String error = av.getErrorDetails(actions, read, read.length, tmpl, 0);
    TestUtils.containsAll(error, "ValidatingEditDistance action problem: action 3: read[2] (1) != template[2] (4) score=0",
        "0|       10|       20|       30|       40|       50|       60|       70|       80|       90|",
        "tmpl:    AATA", "read:    AAAA", "actions: ==== score=0");

    error = av.getErrorDetails(actions, read, read.length, tmpl, 1);
    TestUtils.containsAll(error, "tmpl[0..]: A", "tmpl:    ATAN", "read:    AAAA", "actions: ==== score=0");
  }

  public void testEndInsert() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("aata");
    final int[] actions = ActionsHelper.build("====", 0, 0);
    assertFalse(av.isValid(actions, read, 0, tmpl, Integer.MAX_VALUE));
    final String error = av.getErrorDetails(actions, read, 0, tmpl, 0);
    assertTrue(error.contains("ValidatingEditDistance action problem: non-insert action at end of the read"));
  }

  public void testSubstitutionFail() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("aaaa");
    final int[] actions = ActionsHelper.build("==X=", 0, 0);
    assertFalse(av.isValid(actions, read, read.length, tmpl, Integer.MAX_VALUE));
    final String error = av.getErrorDetails(actions, read, read.length, tmpl, 0);
    assertTrue(error.contains("ValidatingEditDistance action problem: action 3: read[2] (1) == template[2] (1) score=1"));
  }

  public void testFewActionsFail() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("aaaa");
    final int[] actions = ActionsHelper.build("===", 0, 0);
    assertFalse(av.isValid(actions, read, read.length, tmpl, Integer.MAX_VALUE));
    final String error = av.getErrorDetails(actions, read, read.length, tmpl, 0);
    assertTrue(error.contains("ValidatingEditDistance action problem: actions cover only 3 residues, but the read has 4"));
  }

  public void testClaimedScoreMaxIntFail() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("aaaa");
    final int[] actions = ActionsHelper.build("====", 0, Integer.MAX_VALUE);
    assertFalse(av.isValid(actions, read, read.length, tmpl, 63));
    final String error = av.getErrorDetails(actions, read, read.length, tmpl, 0);
    assertTrue(error.contains("ValidatingEditDistance action problem: actual score 0 < max (63) but score was MAX_VALUE"));
  }

  public void testClaimedScoreFail() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("aaaa");
    final int[] actions = ActionsHelper.build("====", 0, 5);
    assertFalse(av.isValid(actions, read, read.length, tmpl, 63));
    final String error = av.getErrorDetails(actions, read, read.length, tmpl, 0);
    assertTrue(error.contains("ValidatingEditDistance action problem: actual score 0 != claimed score 5"));
  }

  public void testClaimedScorePass() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 0);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("aaaa");
    final int[] actions = ActionsHelper.build("====", 0, 0);
    assertTrue(av.isValid(actions, read, read.length, tmpl, 63));
  }

  public void testClaimedScoreNPass() {
    final ActionsValidator av = new ActionsValidator(1, 1, 1, 2);
    final byte[] read = DnaUtils.encodeString("aaaa");
    final byte[] tmpl = DnaUtils.encodeString("agNa");
    final int[] actions = ActionsHelper.build("=XX=", 0, 3);
    assertTrue(av.isValid(actions, read, read.length, tmpl, 63));
  }

  public void testEqual() {
    checkIsValidDna(
        "acgt",
        "acgt");
  }

  /**
   * Test a deletion from the read.
   */
  public void testDelete() {
    checkIsValidDna(
        "acgacgtttcgcgcgc",
        "cgacgcgcgcg");
    }

  /**
   * Two deletes from the read
   */
  public void testDelete2() {
    checkIsValidDna(
        "tactcgaacccttcaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaatatgggtactgcat",
        "tactcga    ttcaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaatat   tactgcat".replaceAll(" ", ""));
  }

  /**
   * Test a single insertion into the read.
   */
  public void testInsert() {
    checkIsValidDna(
        "gatacgaactcgtacgcact",
        "gatacgaacccctcgtacgcactcg");
  }

  /**
   * This is an example where CachedMatrixEditDistance gives an alignment
   * with score 5, and misses the <code>8=3D11=</code> alignment with score 4.
   */
  public void testBadAlignment() {
    checkIsValidDna(
        "gatacgaa" +    "ctcgtcgcact",
        "gatacgaa" + "accctcgtcgcactcg");
  }

  /**
   * Test two insertions.
   */
  public void testInsert2() {
    checkIsValidDna(
           "actcactaagggggtttctatagtttttcactcgg",
        "gggactcactaa" + "tttctatag" + "cactcggggg");
  }

  public void checkIsValidDna(final String readString, final String templateString) {
    checkIsValid(readString, templateString, 1);
  }

  /**
   * Test the ActionsValidator by mutating entries in the actions array.
   *
   * @param readString the read
   * @param templateString the template
   * @param gapOpen the DNA gap open penalty.
   */
  public void checkIsValid(final String readString, final String templateString, final int gapOpen) {
    final int maxScore = Integer.MAX_VALUE;
    final UnidirectionalEditDistance editDist = new GotohEditDistance(gapOpen, 1, 1, 1, false);
    final ActionsValidator validator = new ActionsValidator(gapOpen, 1, 1, 1);
    final byte[] read = readString.getBytes();
    final byte[] temp = templateString.getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    final int msf = Math.max((int) (read.length * 0.5), 7);

    int[] a0 = editDist.calculateEditDistance(read, read.length, temp, 0, maxScore, msf, true);
    a0 = ActionsHelper.copy(a0); // make it minimal.
    assertTrue(validator.isValid(a0, read, read.length, temp, false, maxScore));

    // Now try perturbing each integer in the actions array,
    // and check that every change gives a false result from isValid.
    final int len = ActionsHelper.actionsCount(a0);
    for (int i = 0; i <= ActionsHelper.ACTIONS_START_INDEX + (len - 1) / ActionsHelper.ACTIONS_PER_INT; ++i) {
      //System.err.println("\nPerturbing entry a0[" + i + "] = " + a0[i]);
      a0[i]--;
      assertFalse(validator.mErrorMsg, validator.isValid(a0, read, read.length, temp, false, maxScore));
      a0[i]++;
      assertTrue(validator.mErrorMsg, validator.isValid(a0, read, read.length, temp, false, maxScore));
      a0[i]++;
      assertFalse(validator.isValid(a0, read, read.length, temp, false, maxScore));
      a0[i]--;
      assertTrue(validator.isValid(a0, read, read.length, temp, false, maxScore));
    }
  }


  public void testRcSimple() {
    final String read = "tactcgaaccccttctatggggtactgcat";
    checkIsValidRc(
      read,
      DnaUtils.reverseComplement(read)
    );
  }

  public void testRcWithDelete() {
    checkIsValidRc(
      "acgacgtttcgcgcgc",
      DnaUtils.reverseComplement("cgacgcgcgcg")
    );
  }

  /*
   This test does not work yet - the calculation of RC start position is 2, but should be 0?
   public void testRcWithInsert() {
    checkIsValid(
        "gatacgaactcgtacgcact",
        Utils.reverseComplement("gatacgaacccctcgtacgcactcg"),
        true,
        Integer.valueOf(1)
        );
  }
  */

  private void checkIsValidRc(final String readString, final String templateString) {
    final int maxScore = Integer.MAX_VALUE;
    final UnidirectionalEditDistance editDist = new GotohEditDistance(1, 1, 1, 1, false);
    final ActionsValidator validator = new ActionsValidator(1, 1, 1, 1);
    final byte[] read = readString.getBytes();
    final byte[] temp = templateString.getBytes();
    DnaUtils.encodeArray(read);
    DnaUtils.encodeArray(temp);
    final byte[] tempRc = temp.clone();
    DNA.reverseComplementInPlace(tempRc, 0, tempRc.length);
    final int msf = Math.max((int) (read.length * 0.5), 7);

    int[] a0 = editDist.calculateEditDistance(read, read.length, tempRc, tempRc.length - read.length, maxScore, msf, true);
    if (a0[ActionsHelper.ALIGNMENT_SCORE_INDEX] != Integer.MAX_VALUE) {
      a0[ActionsHelper.TEMPLATE_START_INDEX] = tempRc.length - ActionsHelper.zeroBasedTemplateEndPos(a0);
    }

    a0 = ActionsHelper.copy(a0); // make it minimal.
    assertTrue(validator.isValid(a0, read, read.length, temp, true, maxScore));

    // Now try perturbing each integer in the actions array,
    // and check that every change gives a false result from isValid.
    final int len = ActionsHelper.actionsCount(a0);
    for (int i = 0; i <= ActionsHelper.ACTIONS_START_INDEX + (len - 1) / ActionsHelper.ACTIONS_PER_INT; ++i) {
      //System.err.println("\nPerturbing entry a0[" + i + "] = " + a0[i]);
      a0[i]--;
      assertFalse(validator.mErrorMsg, validator.isValid(a0, read, read.length, temp, true, maxScore));
      a0[i]++;
      assertTrue(validator.mErrorMsg, validator.isValid(a0, read, read.length, temp, true, maxScore));
      a0[i]++;
      assertFalse(validator.isValid(a0, read, read.length, temp, true, maxScore));
      a0[i]--;
      assertTrue(validator.isValid(a0, read, read.length, temp, true, maxScore));
    }
  }
}

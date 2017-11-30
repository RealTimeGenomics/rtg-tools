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

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.mode.DnaUtils;


public abstract class AbstractUnidirectionalEditDistanceTest extends AbstractNanoTest {

  protected abstract UnidirectionalEditDistance getEditDistanceInstance(int gapOpenPenalty, int gapExtendPenalty, int substitutionPenalty, int unknownsPenalty);

  public void testStartPositionSet() throws Exception {
    final byte[] read = DnaUtils.encodeString("acgta");
    final byte[] template = DnaUtils.encodeString("tttttacgtatc");

    final UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 0);

    int[] actions = ed.calculateEditDistance(read, read.length, template, 5, 1, 1, true);

    if (actions != null) {
      assertEquals(0, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(5, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    }

    actions = ed.calculateEditDistance(read, read.length, template, 7, 1, 1, true);
    if (actions != null) {
      assertEquals(Integer.MAX_VALUE, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(7, actions[ActionsHelper.TEMPLATE_START_INDEX]);
    }
  }

  public void testSoftClip() throws Exception {

    final byte[] t = DnaUtils.encodeString("tttaaaaaaaaaaa");
    final byte[] s1 = DnaUtils.encodeString("tttaaaaaaaaaaaa");
    final byte[] t2 = DnaUtils.encodeString("tttaaaaaaa");

    UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 1);

    int[] actions = ed.calculateEditDistance(s1, s1.length, t, 0, 10, 5, false);
    if (actions != null) {
      assertEquals(1, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("==============X", ActionsHelper.toString(actions));
    }

    actions = ed.calculateEditDistance(s1, s1.length, t2, 0, 10, 5, false);
    if (actions != null) {
      assertEquals(5, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("==========XXXXX", ActionsHelper.toString(actions));
    }

    ed = getEditDistanceInstance(1, 1, 1, 1);

    actions = ed.calculateEditDistance(s1, s1.length, t, 0, 10, 5, false);
    if (actions != null) {
      assertEquals(1, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("==============X", ActionsHelper.toString(actions));
    }

    actions = ed.calculateEditDistance(s1, s1.length, t2, 0, 10, 5, false);
    if (actions != null) {
      assertEquals(5, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("==========XXXXX", ActionsHelper.toString(actions));
    }
  }

  public void testalign3Mismatches() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] r = DnaUtils.encodeString("atatatatatatatatatatatatatatataaaa");
    final byte[] t = DnaUtils.encodeString("atatatatatCCatatatataCatatatataaaa");

    final int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 10, 3, false);
    if (actions != null) {
      assertEquals(3, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("==========XX=========X============", ActionsHelper.toString(actions));
    }
  }

  public void testAlignNs() {
    UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] r = DnaUtils.encodeString("atatatatatatatatatatatatatatataaaa");
    final byte[] t = DnaUtils.encodeString("NtatatatatatatatatatatatatNtataNaa");

    int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 10, 3, false);
    if (actions != null) {
      assertEquals(0, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("X=========================X====X==", ActionsHelper.toString(actions));
    }
    ed = getEditDistanceInstance(1, 1, 1, 1);
    actions = ed.calculateEditDistance(r, r.length, t, 0, 10, 5, false);
    if (actions != null) {
      assertEquals(3, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("X=========================X====X==", ActionsHelper.toString(actions));
    }
  }

  public void testAlignNs2() {
    UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] r = DnaUtils.encodeString("atatatatatatatatatatatNtatatataNaa");
    final byte[] t = DnaUtils.encodeString("atatatatatNNatatatataNatatatataNaa");

    int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 10, 3, false);
    if (actions != null) {
      assertEquals(0, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("==========XX=========XX========X==", ActionsHelper.toString(actions));
    }

    ed = getEditDistanceInstance(1, 1, 1, 1);

    actions = ed.calculateEditDistance(r, r.length, t, 0, 10, 3, false);
    if (actions != null) {
      assertEquals(5, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("==========XX=========XX========X==", ActionsHelper.toString(actions));
    }
  } //*/

  public void testPenaltiesMismatch() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 1, 9, 0);

    final byte[] r = DnaUtils.encodeString("AtttttgttttCtgctgacatgactgacgatcgatgctagctgatcgaca");
    final byte[] t = DnaUtils.encodeString("gtttttgttttgtgctgacatgactgacgatcgatgctagctgatcgaca");

    final int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 20, 5, true);
    if (actions != null) {
      assertEquals(18, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("X==========X======================================", ActionsHelper.toString(actions));
    }
  }
  public void testPenaltiesInsert1Len() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 1, 9, 0);

    final byte[] r = DnaUtils.encodeString("gtttttgtttatgtgctgacatgactgacgatcgatgctagctgatcgaca");
    final byte[] t = DnaUtils.encodeString("gtttttgttta gtgctgacatgactgacgatcgatgctagctgatcgaca".replaceAll(" ", ""));

    final int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 20, 5, true);
    if (actions != null) {
      assertEquals(20, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("===========I=======================================", ActionsHelper.toString(actions));
    }
  }
  public void testPenaltiesInsert2Len() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 1, 9, 0);

    final byte[] r = DnaUtils.encodeString("gtttttgtttattgtgctgacatgactgacgatcgatgctagctgatcgaca");
    final byte[] t = DnaUtils.encodeString("gtttttgttta  gtgctgacatgactgacgatcgatgctagctgatcgaca".replaceAll(" ", ""));

    final int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 21, 5, true);
    if (actions != null) {
      assertEquals(21, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("===========II=======================================", ActionsHelper.toString(actions));
    }
  }
  public void testPenaltiesDel2Len() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 1, 9, 0);

    final byte[] r = DnaUtils.encodeString("gtttttgttta  gtgctgacatgactgacgatcgatgctagctgatcgaca".replaceAll(" ", ""));
    final byte[] t = DnaUtils.encodeString("gtttttgtttattgtgctgacatgactgacgatcgatgctagctgatcgaca");

    final int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 21, 5, true);
    if (actions != null) {
      assertEquals(21, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("===========DD=======================================", ActionsHelper.toString(actions));
    }
  }

  public void testPenaltiesAll() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(19, 2, 9, 0);

    final byte[] r = DnaUtils.encodeString("TTTTTTTTGTAGTTTTTAC  TGATGGATGGATGGACAGGTAGGCAGGTA".replaceAll(" ", ""));
    final byte[] t = DnaUtils.encodeString("TTTTTTTTGTACTTTTTACGGTGATGGATGGATGGACAG TAGGCAGGTA".replaceAll(" ", ""));

    final int[] actions = ed.calculateEditDistance(r, r.length, t, 0, 53, 10, false);
    if (actions != null) {
      assertEquals(53, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(0, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("===========X=======DD=================I===========", ActionsHelper.toString(actions));
    }
  }

  public void testEarlyTerminationCondition() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 0);

    final byte[] r = DnaUtils.encodeString("  gtggatntatatatcTatatatatacatatatataaaa".replaceAll(" ", ""));
    final byte[] t = DnaUtils.encodeString("aagtggatatatatatccatatatatacatatatataaaa");

    int[] actions = ed.calculateEditDistance(r, r.length, t, 2, 0, 3, false);
    if (actions != null) {
      assertEquals(Integer.MAX_VALUE, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(2, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("", ActionsHelper.toString(actions));
    }
    final byte[] r2 = DnaUtils.encodeString("gtggatatntatatcTCtatatatacatatatataaaa");
    actions = ed.calculateEditDistance(r2, r2.length, t, 2, 1, 3, false);
    if (actions != null) {
      assertEquals(Integer.MAX_VALUE, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(2, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("", ActionsHelper.toString(actions));
    }
  }
  public void testEarlyTerminationConditionNsAsMismatches() {
    final UnidirectionalEditDistance ed = getEditDistanceInstance(1, 1, 1, 1);

    final byte[] r = DnaUtils.encodeString("  gtggatatatatatcNatatatatacatatatataaaa".replaceAll(" ", ""));
    final byte[] t = DnaUtils.encodeString("aagtggatatatatatccatatatatacatatatataaaa");

    int[] actions = ed.calculateEditDistance(r, r.length, t, 2, 0, 3, false);
    if (actions != null) {
      assertEquals(Integer.MAX_VALUE, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(2, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("", ActionsHelper.toString(actions));
    }
    final byte[] r2 = DnaUtils.encodeString("gtggatatatatatcNNtatatatacatatatataaaa");
    actions = ed.calculateEditDistance(r2, r2.length, t, 2, 1, 3, false);
    if (actions != null) {
      assertEquals(Integer.MAX_VALUE, actions[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
      assertEquals(2, actions[ActionsHelper.TEMPLATE_START_INDEX]);
      assertEquals("", ActionsHelper.toString(actions));
    }
  }
}

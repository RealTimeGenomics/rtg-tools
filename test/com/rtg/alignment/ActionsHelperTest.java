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
package com.rtg.alignment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.rtg.mode.ProteinScoringMatrix;
import com.rtg.mode.Protein;
import com.rtg.util.InvalidParamsException;

import junit.framework.TestCase;

/**
 * Test class.
 */
public class ActionsHelperTest extends TestCase {

  static final int BITS = ActionsHelper.BITS_PER_ACTION;

  public ActionsHelperTest(final String name) {
    super(name);
  }

  private int[] cycle(final int length) {
    final int plen = ActionsHelper.ACTIONS_START_INDEX + ((length + ActionsHelper.ACTIONS_PER_INT - 1) / ActionsHelper.ACTIONS_PER_INT);
    final int[] sb = new int[plen];
    sb[ActionsHelper.ACTIONS_LENGTH_INDEX] = length;
    final int cmds0123 = (0 << 3 * BITS) + (1 << 2 * BITS) + (2 << 1 * BITS) + (3 << 0 * BITS);
    final int cmds01230123 = (cmds0123 << 4 * BITS) + cmds0123;
    Arrays.fill(sb, ActionsHelper.ACTIONS_START_INDEX, sb.length, cmds01230123);
    final int rem = length & ActionsHelper.ACTIONS_COUNT_MASK;
    if (rem != 0) {
      sb[sb.length - 1] &= ~0 << (32 - ActionsHelper.BITS_PER_ACTION * rem);
    }
    return sb;
  }

  public void testCopy() {
    final int[] a = cycle(20);
    assertEquals(20, ActionsHelper.actionsCount(a));
    assertTrue(Arrays.equals(a, ActionsHelper.copy(a)));
  }

  public void testMatchCount() {
    assertEquals(0, ActionsHelper.matchCount(cycle(0)));
    assertEquals(1, ActionsHelper.matchCount(cycle(1)));
    assertEquals(4, ActionsHelper.matchCount(cycle(16)));
    assertEquals(200 / 4, ActionsHelper.matchCount(cycle(200)));
    assertEquals(10, ActionsHelper.matchCount(ActionsHelper.build("==X==X==X==D==I", 0, 0)));
    assertEquals(1, ActionsHelper.matchCount(ActionsHelper.build("=XXXXXXXXIXX", 0, 0)));
    assertEquals(29, ActionsHelper.matchCount(ActionsHelper.build("====================IIIII===III===XXX===", 0, 0)));
    assertEquals(16, ActionsHelper.matchCount(ActionsHelper.build("================", 0, 0)));
    assertEquals(10, ActionsHelper.matchCount(ActionsHelper.build("=B=B=====NNN==N=", 0, 0)));
    assertEquals(4, ActionsHelper.matchCount(ActionsHelper.build("=XDXIN==XBXIN=", 0, 0)));
  }

  public void testDeletionFromReadCount() {
    assertEquals(0, ActionsHelper.deletionFromReadAndOverlapCount(cycle(0)));
    assertEquals(0, ActionsHelper.deletionFromReadAndOverlapCount(cycle(1)));
    assertEquals(ActionsHelper.toString(cycle(16)), 4, ActionsHelper.deletionFromReadAndOverlapCount(cycle(16)));
    assertEquals(4, ActionsHelper.deletionFromReadAndOverlapCount(cycle(17)));
    assertEquals(200 / 4, ActionsHelper.deletionFromReadAndOverlapCount(cycle(200)));
    assertEquals(29, ActionsHelper.deletionFromReadAndOverlapCount(ActionsHelper.build("IIIIIIIIIIIIIIIIIIIIDDDDDIIIDDDIIIXXXIII", 0, 0)));
    assertEquals(16, ActionsHelper.deletionFromReadAndOverlapCount(ActionsHelper.build("IIIIIIIIIIIIIIII", 0, 0)));
    assertEquals(16, ActionsHelper.deletionFromReadAndOverlapCount(ActionsHelper.build("IIIIIIBBBIIINNNNNIIII", 0, 0)));
  }

  public void testInsertionIntoReadCount() {
    assertEquals(7, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("==X=DDDDDD=X==X==D==I", 0, 0)));
    assertEquals(0, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("=XXXXXXXXIXX", 0, 0)));
    assertEquals(2, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("=XXXDDXXXXXIXX", 0, 0)));
    assertEquals(3, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("=XXIXDIIXXDXXXIXDX", 0, 0)));
    assertEquals(6, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("=DXDIIXXIIXDIXDXXDXIIXDX", 0, 0)));
    assertEquals(29, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("DDDDDDBBBDDDDDDDDDDDDDD=====DDD===DDDXXXDDD", 0, 0)));
    assertEquals(16, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("DDDDDDDDDDDDDDDD", 0, 0)));
    assertEquals(16, ActionsHelper.insertionIntoReadAndGapCount(ActionsHelper.build("DDDDDDDNNNNNNNDD", 0, 0)));
  }

  public void testTemplateLength() {
    assertEquals(5, ActionsHelper.templateLength(ActionsHelper.build("=====", 0, 0)));
    assertEquals(5, ActionsHelper.templateLength(ActionsHelper.build("=XXX=", 0, 0)));
    assertEquals(10, ActionsHelper.templateLength(ActionsHelper.build("=XDNRT====", 0, 0)));
    assertEquals(10, ActionsHelper.templateLength(ActionsHelper.build("=XDNRT=IISS===", 0, 0)));
    assertEquals(9, ActionsHelper.templateLength(ActionsHelper.build("=XDNRT=IISS=B==", 0, 0)));
  }

  public void testReadLength() {
    assertEquals(5, ActionsHelper.readLength(ActionsHelper.build("=====", 0, 0)));
    assertEquals(5, ActionsHelper.readLength(ActionsHelper.build("=XXX=", 0, 0)));
    assertEquals(10, ActionsHelper.readLength(ActionsHelper.build("=XISRT====", 0, 0)));
    assertEquals(10, ActionsHelper.readLength(ActionsHelper.build("=XISRT==DDNN==", 0, 0)));
  }

  public void testIndelLength() {
    assertEquals(6, ActionsHelper.indelLength(ActionsHelper.build("==X=DDDDDD=X==X==D==I", 0, 0)));
    assertEquals(-1, ActionsHelper.indelLength(ActionsHelper.build("=XXXXXXXXIXX", 0, 0)));
    assertEquals(1, ActionsHelper.indelLength(ActionsHelper.build("=XXXDDXXXXXIXX", 0, 0)));
    assertEquals(-1, ActionsHelper.indelLength(ActionsHelper.build("=XXIXDIIXXDXXXIXDX", 0, 0)));
    assertEquals(-1, ActionsHelper.indelLength(ActionsHelper.build("=DXDIIXXIIXDIXDXXDXIIXDX", 0, 0)));
    assertEquals(-1, ActionsHelper.indelLength(ActionsHelper.build("=DXDIIXXIIXDIXDX", 0, 0)));
    assertEquals(3, ActionsHelper.indelLength(ActionsHelper.build("=DXNNNNNIIXXBBXDIXDX", 0, 0)));
  }
//
//  public void testSubCount() {
//    assertEquals(3, ActionsHelper.subCount(ActionsHelper.build("==X==X==X==D==I", 0, 0)));
//    assertEquals(1, ActionsHelper.subCount(ActionsHelper.build("X===============", 0, 0)));
//    assertEquals(29, ActionsHelper.subCount(ActionsHelper.build("XXXXXXXXXXXXXXXXXXXX=====XXX===XXXIIIXXX", 0, 0)));
//    assertEquals(16, ActionsHelper.subCount(ActionsHelper.build("XXXXXXXXXXXXXXXX", 0, 0)));
//    assertEquals(3, ActionsHelper.subCount(ActionsHelper.build("=BX==XNNX==DBBI", 0, 0)));
//
//    assertEquals(0, ActionsHelper.subCount(new int[2]));
//  }

  public void testIsCg() {
    assertFalse(ActionsHelper.isCg(new int[2]));
    assertFalse(ActionsHelper.isCg(ActionsHelper.build("=X==X==X==D==I", 0, 0)));
    assertTrue(ActionsHelper.isCg(ActionsHelper.build("==XB=X==X==D==I", 0, 0)));
    assertTrue(ActionsHelper.isCg(ActionsHelper.build("N=X==X==X==D==I", 0, 0)));
    assertTrue(ActionsHelper.isCg(ActionsHelper.build("==X==X==X==D==N", 0, 0)));
    assertTrue(ActionsHelper.isCg(ActionsHelper.build("BNBNBNBNBNBNBNB", 0, 0)));
  }

  public void testWriteProtein() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      ActionsHelper.writeProtein(bos, Protein.encodeProteins("ACDE"), 0, cycle(4), ActionsHelper.INSERTION_INTO_REFERENCE);
    } finally {
      bos.close();
    }
    assertEquals("-acd", bos.toString());
  }
  public void testWriteProteinLong() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      final String prot = "ARNDCQEGHILKMFPSTWYV*";
      ActionsHelper.writeProtein(bos, Protein.encodeProteins(prot), 0, cycle(prot.length()), ActionsHelper.INSERTION_INTO_REFERENCE);
    } finally {
      bos.close();
    }
    assertEquals("a-rnd-cqe-ghi-lkm-fps", bos.toString());
  }

  public void testWriteProteinOffArray() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      ActionsHelper.writeProtein(bos, Protein.encodeProteins("E"), -2, cycle(5), ActionsHelper.INSERTION_INTO_REFERENCE);
    } finally {
      bos.close();
    }
    assertEquals("x-xex", bos.toString());
  }

  public void testWriteMatches1() throws IOException, InvalidParamsException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final byte[] p = Protein.encodeProteins("ACDE");
    try {
      assertEquals(2, ActionsHelper.writeProteinMatches(bos, p, p, cycle(4), new ProteinScoringMatrix()));
    } finally {
      bos.close();
    }
    assertEquals("  +d", bos.toString());
  }

  public void testWriteMatches2() throws IOException, InvalidParamsException {
    final int[] sb = {0, 0, 21, 0, 0, 0};
    final byte[] p = Protein.encodeProteins("ARNDCQEGHILKMFPSTWYV*");
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      assertEquals(21, ActionsHelper.writeProteinMatches(bos, p, p, sb, new ProteinScoringMatrix()));
    } finally {
      bos.close();
    }
    assertEquals("arndcqeghilkmfpstwyv*", bos.toString());
  }

  public void testWriteMatches3() throws IOException, InvalidParamsException {
    final int[] sb = {0, 0, 5,
                                (ActionsHelper.MISMATCH << 28)
                                + (ActionsHelper.SAME << 24)
                                + (ActionsHelper.SAME << 20)
                                + (ActionsHelper.DELETION_FROM_REFERENCE << 16)
                                + (ActionsHelper.MISMATCH << 12)};
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      assertEquals(3, ActionsHelper.writeProteinMatches(bos, Protein.encodeProteins("ACDE"), Protein.encodeProteins("QFCDD"), sb, new ProteinScoringMatrix()));
    } finally {
      bos.close();
    }
    assertEquals("  cd+", bos.toString());
  }

  static final int[] ACTIONS_ARRAY = {0, 0, 23,
                                (((((((ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.SAME << BITS)
                                + ActionsHelper.SAME << BITS)
                                + ActionsHelper.DELETION_FROM_REFERENCE << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH
                                ,
                                (((((((ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.UNKNOWN_READ
                                ,
                               ((((((ActionsHelper.UNKNOWN_TEMPLATE << BITS)
                               + ActionsHelper.SAME << BITS)
                               + ActionsHelper.SAME << BITS)
                               + ActionsHelper.DELETION_FROM_REFERENCE << BITS)
                               + ActionsHelper.MISMATCH << BITS)
                               + ActionsHelper.MISMATCH << BITS)
                               + ActionsHelper.SOFT_CLIP << BITS
    };
  public void testBuild() {
    final int[] sb = {0, 0, 5,
                                (((((ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.SAME << BITS)
                                + ActionsHelper.SAME << BITS)
                                + ActionsHelper.DELETION_FROM_REFERENCE << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                << BITS * 2};
    int[] built = ActionsHelper.build("XD==X", 0, 0);

    actionsEquals(sb, built);
    built = ActionsHelper.build("SXXD==TRXXXXXXXXXXXD==X", 0, 0);

    actionsEquals(ACTIONS_ARRAY, built);
  }

  public void actionsEquals(final int[] subject, final int[] query) {
    assertEquals(subject.length, query.length);
    for (int i = 0; i < subject.length; ++i) {
      assertEquals(subject[i], query[i]);
    }
  }

  public void testToString() {
    final int[] sb = {0, 0, 5,
                                (((((ActionsHelper.MISMATCH << BITS)
                                + ActionsHelper.SAME << BITS)
                                + ActionsHelper.SAME << BITS)
                                + ActionsHelper.DELETION_FROM_REFERENCE << BITS)
                                + ActionsHelper.MISMATCH << BITS)
                                << BITS * 2};
    assertEquals("XD==X", ActionsHelper.toString(sb));
    assertEquals("SXXD==TRXXXXXXXXXXXD==X", ActionsHelper.toString(ACTIONS_ARRAY));

    final String one = "X========X===I=====D=====I====D======X";
    assertEquals(one, ActionsHelper.toString(ActionsHelper.build(one, 0, 0)));

    assertEquals(1, ActionsHelper.insertionIntoReadAndGapCount(sb));
    assertEquals(0, ActionsHelper.deletionFromReadAndOverlapCount(sb));
  }

  public void testIterator() {
    assertEquals(23, ActionsHelper.actionsCount(ACTIONS_ARRAY));
    final ActionsHelper.CommandIterator iter = ActionsHelper.iterator(ACTIONS_ARRAY);
    next(iter, ActionsHelper.SOFT_CLIP);
    next(iter, ActionsHelper.MISMATCH);
    next(iter, ActionsHelper.MISMATCH);
    next(iter, ActionsHelper.DELETION_FROM_REFERENCE);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.UNKNOWN_TEMPLATE);
    next(iter, ActionsHelper.UNKNOWN_READ);
    for (int i = 0; i < 11; ++i) {
      next(iter, ActionsHelper.MISMATCH);
    }
    next(iter, ActionsHelper.DELETION_FROM_REFERENCE);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.MISMATCH);
    assertFalse(iter.hasNext());
  }

  public void testIteratorEmpty() {
    final int[] actions = {0, 0, 0};
    assertEquals(0, ActionsHelper.actionsCount(actions));
    assertFalse(ActionsHelper.iterator(actions).hasNext());
    assertFalse(ActionsHelper.iteratorReverse(actions).hasNext());
  }

  public void testIteratorReverse() {
    assertEquals(23, ActionsHelper.actionsCount(ACTIONS_ARRAY));
    final ActionsHelper.CommandIterator iter = ActionsHelper.iteratorReverse(ACTIONS_ARRAY);
    next(iter, ActionsHelper.MISMATCH);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.DELETION_FROM_REFERENCE);
    for (int i = 0; i < 11; ++i) {
      next(iter, ActionsHelper.MISMATCH);
    }
    next(iter, ActionsHelper.UNKNOWN_READ);
    next(iter, ActionsHelper.UNKNOWN_TEMPLATE);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.SAME);
    next(iter, ActionsHelper.DELETION_FROM_REFERENCE);
    next(iter, ActionsHelper.MISMATCH);
    next(iter, ActionsHelper.MISMATCH);
    next(iter, ActionsHelper.SOFT_CLIP);
    assertFalse(iter.hasNext());
  }

  private void next(ActionsHelper.CommandIterator iter, int expected) {
    assertTrue(iter.hasNext());
    assertEquals(expected, iter.next());
  }

  public void testPrependArray() {
    final int[] workspace = new int[1000];
    workspace[ActionsHelper.ALIGNMENT_SCORE_INDEX] = 0;
    workspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = 0;
    workspace[ActionsHelper.TEMPLATE_START_INDEX] = 0;

    // Insert some rubbish to prove reusability
    workspace[ActionsHelper.ACTIONS_START_INDEX] = 0x33;
    workspace[ActionsHelper.ACTIONS_START_INDEX + 2] = 0x33;

    final int[] initial = ActionsHelper.build("I=X==X==X==D==I", 50, 4);
    ActionsHelper.prepend(workspace, initial);
    assertEquals(ActionsHelper.toString(initial), ActionsHelper.toString(workspace));
    assertEquals(4, ActionsHelper.alignmentScore(workspace));
    assertEquals(50, workspace[ActionsHelper.TEMPLATE_START_INDEX]);
    assertEquals(50, ActionsHelper.zeroBasedTemplateStart(workspace));

    final int[] second = ActionsHelper.build("====X", 45, 1);
    ActionsHelper.prepend(workspace, second);
    assertEquals("====XI=X==X==X==D==I", ActionsHelper.toString(workspace));
    assertEquals(5, ActionsHelper.alignmentScore(workspace));
    assertEquals(45, workspace[ActionsHelper.TEMPLATE_START_INDEX]);

    final int[] third = ActionsHelper.build("=D==I", 41, 4);
    ActionsHelper.prepend(workspace, third);
    assertEquals("=D==I====XI=X==X==X==D==I", ActionsHelper.toString(workspace));
    assertEquals(9, ActionsHelper.alignmentScore(workspace));
    assertEquals(41, workspace[ActionsHelper.TEMPLATE_START_INDEX]);

    ActionsHelper.setZeroBasedTemplateStart(workspace, 2);
    assertEquals(2, ActionsHelper.zeroBasedTemplateStart(workspace));
    ActionsHelper.setAlignmentScore(workspace, 5);
    assertEquals(5, ActionsHelper.alignmentScore(workspace));

    ActionsHelper.clear(workspace);
    assertEquals(0, ActionsHelper.actionsCount(workspace));
    assertEquals("", ActionsHelper.toString(workspace));
  }

  public void testPrependTo16Long() {
    final int[] workspace = new int[1000];
    workspace[ActionsHelper.ALIGNMENT_SCORE_INDEX] = 0;
    workspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = 0;
    workspace[ActionsHelper.TEMPLATE_START_INDEX] = 90;
    workspace[ActionsHelper.ACTIONS_START_INDEX] = Integer.MAX_VALUE;
    workspace[ActionsHelper.ACTIONS_START_INDEX + 1] = Integer.MAX_VALUE;

    ActionsHelper.prepend(workspace, 16, ActionsHelper.SAME, 0);
    assertEquals(16, workspace[ActionsHelper.ACTIONS_LENGTH_INDEX]);
    final int[] initial = ActionsHelper.build("==X==X==X==D===I", 50, 7);
    ActionsHelper.prepend(workspace, initial);
    assertEquals("==X==X==X==D===I================", ActionsHelper.toString(workspace));
    assertEquals(7, ActionsHelper.alignmentScore(workspace));
    assertEquals(50, workspace[ActionsHelper.TEMPLATE_START_INDEX]);
  }

  public void testPrependLongArray() {
    final int[] workspace = new int[1000];
    workspace[ActionsHelper.ALIGNMENT_SCORE_INDEX] = 0;
    workspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = 0;
    workspace[ActionsHelper.TEMPLATE_START_INDEX] = 90;
    workspace[ActionsHelper.ACTIONS_START_INDEX] = Integer.MAX_VALUE;
    workspace[ActionsHelper.ACTIONS_START_INDEX + 1] = Integer.MAX_VALUE;

    ActionsHelper.prepend(workspace, 16, ActionsHelper.SAME, 0);
    assertEquals(16, workspace[ActionsHelper.ACTIONS_LENGTH_INDEX]);
    final int[] initial = ActionsHelper.build("==X==X==X==D===I=========III==========DDD", 50, 15);
    ActionsHelper.prepend(workspace, initial);
    assertEquals("==X==X==X==D===I=========III==========DDD================", ActionsHelper.toString(workspace));
    assertEquals(15, ActionsHelper.alignmentScore(workspace));
    assertEquals(50, workspace[ActionsHelper.TEMPLATE_START_INDEX]);
  }
  public void testPrependCounts() {
    final int[] workspace = new int[1000];
    workspace[ActionsHelper.ALIGNMENT_SCORE_INDEX] = 0;
    workspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = 0;
    workspace[ActionsHelper.TEMPLATE_START_INDEX] = 90;

    // Insert some rubish to prove reusability
    workspace[ActionsHelper.ACTIONS_START_INDEX] = 0xFF;
    workspace[ActionsHelper.ACTIONS_START_INDEX + 2] = 0xFF;

    ActionsHelper.prepend(workspace, 5, ActionsHelper.SAME, 0);
    assertEquals("=====", ActionsHelper.toString(workspace));

    ActionsHelper.prepend(workspace, 3, ActionsHelper.MISMATCH, 3);
    assertEquals("XXX=====", ActionsHelper.toString(workspace));

    ActionsHelper.prepend(workspace, 2, ActionsHelper.INSERTION_INTO_REFERENCE, 4);
    assertEquals("IIXXX=====", ActionsHelper.toString(workspace));

    ActionsHelper.prepend(workspace, 5, ActionsHelper.SAME, 0);
    assertEquals("=====IIXXX=====", ActionsHelper.toString(workspace));

    ActionsHelper.prepend(workspace, 1, ActionsHelper.SAME, 0);
    assertEquals("======IIXXX=====", ActionsHelper.toString(workspace));

    ActionsHelper.prepend(workspace, 1, ActionsHelper.SAME, 0);
    assertEquals("=======IIXXX=====", ActionsHelper.toString(workspace));

    ActionsHelper.prepend(workspace, 3, ActionsHelper.DELETION_FROM_REFERENCE, 6);
    assertEquals("DDD=======IIXXX=====", ActionsHelper.toString(workspace));
    assertEquals(13, ActionsHelper.alignmentScore(workspace));

    ActionsHelper.prepend(workspace, 36, ActionsHelper.MISMATCH, 6);
    assertEquals("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXDDD=======IIXXX=====", ActionsHelper.toString(workspace));
    assertEquals(19, ActionsHelper.alignmentScore(workspace));
    assertEquals(36, workspace[ActionsHelper.TEMPLATE_START_INDEX]);

    ActionsHelper.prepend(workspace, 36, ActionsHelper.INSERTION_INTO_REFERENCE, 6);
    assertEquals("IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII"
            + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXDDD=======IIXXX=====", ActionsHelper.toString(workspace));
  }

  public void testPrependHighCount() {
    final int[] workspace = new int[1000];
    workspace[ActionsHelper.ALIGNMENT_SCORE_INDEX] = 0;
    workspace[ActionsHelper.ACTIONS_LENGTH_INDEX] = 0;
    workspace[ActionsHelper.TEMPLATE_START_INDEX] = 90;

    // Insert some rubish to prove reusability
    for (int i = ActionsHelper.ACTIONS_START_INDEX; i < workspace.length; ++i) {
      workspace[i] = 0xFF;
    }
    ActionsHelper.prepend(workspace, 36, ActionsHelper.INSERTION_INTO_REFERENCE, 6);
    ActionsHelper.prepend(workspace, 36, ActionsHelper.DELETION_FROM_REFERENCE, 6);
    ActionsHelper.prepend(workspace, 36, ActionsHelper.MISMATCH, 6);
    ActionsHelper.prepend(workspace, 36, ActionsHelper.SAME, 6);
    assertEquals(
        "===================================="
        + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
        + "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD"
        + "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII"
            , ActionsHelper.toString(workspace));
  }

  public void testReverse() {
    final int[] acts0 = ActionsHelper.build("=XXB==DDDD===IIINN", 11, 12);
    int[] acts = ActionsHelper.copy(acts0);
    ActionsHelper.reverse(acts);
    assertEquals(11, ActionsHelper.zeroBasedTemplateStart(acts));
    assertEquals(12, ActionsHelper.alignmentScore(acts));
    assertEquals("NNIII===DDDD==BXX=", ActionsHelper.toString(acts));
    ActionsHelper.reverse(acts); // double reverse equals a nop.
    assertTrue(Arrays.equals(acts0, acts));

    acts = ActionsHelper.build("DX", 1, 2);
    ActionsHelper.reverse(acts);
    assertEquals("XD", ActionsHelper.toString(acts));
  }

  public void testActionToChar() {
    assertEquals('.', ActionsHelper.actionToChar(ActionsHelper.SAME));
    assertEquals('X', ActionsHelper.actionToChar(ActionsHelper.MISMATCH));
    assertEquals('D', ActionsHelper.actionToChar(ActionsHelper.DELETION_FROM_REFERENCE));
    assertEquals('I', ActionsHelper.actionToChar(ActionsHelper.INSERTION_INTO_REFERENCE));
    assertEquals('N', ActionsHelper.actionToChar(ActionsHelper.CG_GAP_IN_READ));
    assertEquals('S', ActionsHelper.actionToChar(ActionsHelper.SOFT_CLIP));
    assertEquals('B', ActionsHelper.actionToChar(ActionsHelper.CG_OVERLAP_IN_READ));
    assertEquals((char) -1, ActionsHelper.actionToChar(5));
    assertEquals((char) -1, ActionsHelper.actionToChar(6));
    assertEquals((char) -1, ActionsHelper.actionToChar(ActionsHelper.NOOP));
    assertEquals((char) -1, ActionsHelper.actionToChar(8));
    assertEquals((char) -1, ActionsHelper.actionToChar(9));
    try {
      ActionsHelper.actionToChar(-1);
      fail();
    } catch (final IndexOutOfBoundsException e) {
      // ok
    }
  }

  public void testSoftClip() {

    int[] soft = ActionsHelper.build("==========", 0, 0);
    ActionsHelper.softClip(soft, true, 0, 0);
    assertEquals(0, ActionsHelper.zeroBasedTemplateStart(soft));
    assertEquals("==========", ActionsHelper.toString(soft));

    soft = ActionsHelper.build("==I=======", 0, 2);
    ActionsHelper.softClip(soft, true, 3, 0);
    assertEquals(0, ActionsHelper.zeroBasedTemplateStart(soft));  //doesn't update start pos
    assertEquals("SSS=======", ActionsHelper.toString(soft));

    soft = ActionsHelper.build("====I========", 0, 2);
    ActionsHelper.softClip(soft, true, 5, 0);
    assertEquals("SSSSS========", ActionsHelper.toString(soft));

    soft = ActionsHelper.build("=====I======", 0, 2);
    ActionsHelper.softClip(soft, true, 0, 0);
    assertEquals("=====I======", ActionsHelper.toString(soft));

    soft = ActionsHelper.build("====II======", 0, 3);
    ActionsHelper.softClip(soft, true, 6, 0);
    assertEquals("SSSSSS======", ActionsHelper.toString(soft));

    soft = ActionsHelper.build("====III=====", 0, 3);
    ActionsHelper.softClip(soft, true, 7, 0);
    assertEquals("SSSSSSS=====", ActionsHelper.toString(soft));
    assertFalse(ActionsHelper.isCg(soft));

    soft = ActionsHelper.build("==D======", 0, 3);
    ActionsHelper.softClip(soft, true, 2, 1);
    assertEquals("SS======", ActionsHelper.toString(soft));
    assertFalse(ActionsHelper.isCg(soft));

    soft = ActionsHelper.build("==DD=====", 0, 3);
    ActionsHelper.softClip(soft, true, 2, 2);
    assertEquals("SS=====", ActionsHelper.toString(soft));

    soft = ActionsHelper.build("=====DD==", 0, 3);
    ActionsHelper.softClip(soft, false, 2, 2);
    assertEquals("=====SS", ActionsHelper.toString(soft));
    assertFalse(ActionsHelper.isCg(soft));


    soft = ActionsHelper.build("=DI===II=N==XRTB=D=", 0, 2);
    ActionsHelper.softClip(soft, true, 2, 1);
    assertEquals("SS===II=N==XRTB=D=", ActionsHelper.toString(soft));
    assertEquals(2, ActionsHelper.insertionIntoReadAndGapCount(soft));
    assertEquals(3, ActionsHelper.deletionFromReadAndOverlapCount(soft));
  }

//  public void testPrependOverflow() {
//    final int[] acts0 = ActionsHelper.build("=====", 0, Integer.MAX_VALUE - 1);
//    ActionsHelper.prepend(acts0, 2, ActionsHelper.MISMATCH, 2);
//    assertEquals(Integer.MAX_VALUE, acts0[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
//  }
//
//  public void testPrependOverflow2() {
//    final int[] workspace = new int[1000];
//    final int[] acts0 = ActionsHelper.build("=====", 0, Integer.MAX_VALUE - 1);
//    final int[] acts1 = ActionsHelper.build("=====", 0, Integer.MAX_VALUE - 1);
//    ActionsHelper.prepend(workspace, acts0);
//    assertEquals(acts0[ActionsHelper.ALIGNMENT_SCORE_INDEX], workspace[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
//    ActionsHelper.prepend(workspace, acts1);
//    assertEquals(Integer.MAX_VALUE, workspace[ActionsHelper.ALIGNMENT_SCORE_INDEX]);
//  }

  public void testLengthOnReference() throws Exception {
    assertEquals(5, ActionsHelper.zeroBasedTemplateEndPos(ActionsHelper.build("=====", 0, 0)));
    assertEquals(5, ActionsHelper.zeroBasedTemplateEndPos(ActionsHelper.build("==X==", 0, 0)));
    assertEquals(6, ActionsHelper.zeroBasedTemplateEndPos(ActionsHelper.build("==X==", 1, 0)));
    assertEquals(7, ActionsHelper.zeroBasedTemplateEndPos(ActionsHelper.build("==D===", 1, 0)));
    assertEquals(5, ActionsHelper.zeroBasedTemplateEndPos(ActionsHelper.build("==I==", 1, 0)));
    assertEquals(6, ActionsHelper.zeroBasedTemplateEndPos(ActionsHelper.build("==II=D=", 1, 0)));

    assertEquals(5, ActionsHelper.zeroBasedTemplateEndPos(ActionsHelper.build("SS=====SS", 0, 0)));
  }
}

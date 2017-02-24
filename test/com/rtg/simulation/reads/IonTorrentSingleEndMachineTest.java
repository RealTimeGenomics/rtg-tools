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

package com.rtg.simulation.reads;

import com.rtg.mode.DnaUtils;
import com.rtg.reader.SdfId;
import com.rtg.util.StringUtils;

import junit.framework.TestCase;

/**
 */
public class IonTorrentSingleEndMachineTest extends TestCase {

  public void testHomopolymerLength() {

    final byte[] data = DnaUtils.encodeString("GAAACCTCGTAAGTTTTTTTCTTTCTCTC");

    assertEquals(1, IonTorrentSingleEndMachine.homopolymerLength(0, data, data.length));
    assertEquals(3, IonTorrentSingleEndMachine.homopolymerLength(1, data, data.length));
    assertEquals(3, IonTorrentSingleEndMachine.homopolymerLength(2, data, data.length));
    assertEquals(3, IonTorrentSingleEndMachine.homopolymerLength(3, data, data.length));
    assertEquals(2, IonTorrentSingleEndMachine.homopolymerLength(4, data, data.length));
    assertEquals(2, IonTorrentSingleEndMachine.homopolymerLength(5, data, data.length));
    assertEquals(1, IonTorrentSingleEndMachine.homopolymerLength(6, data, data.length));
    assertEquals(1, IonTorrentSingleEndMachine.homopolymerLength(7, data, data.length));
    assertEquals(1, IonTorrentSingleEndMachine.homopolymerLength(8, data, data.length));
    assertEquals(1, IonTorrentSingleEndMachine.homopolymerLength(9, data, data.length));
    assertEquals(2, IonTorrentSingleEndMachine.homopolymerLength(10, data, data.length));
    assertEquals(2, IonTorrentSingleEndMachine.homopolymerLength(11, data, data.length));
    assertEquals(1, IonTorrentSingleEndMachine.homopolymerLength(12, data, data.length));
    assertEquals(7, IonTorrentSingleEndMachine.homopolymerLength(13, data, data.length));

  }


  public void testRandomHomopoly() throws Exception {
    final IonTorrentSingleEndMachine m = new IonTorrentSingleEndMachine(1312349873);
    m.reseedErrorRandom(1312349873);
    m.setMinSize(5);
    m.setMaxSize(20);
    m.updateWorkingSpace(10);

    final byte[] data = DnaUtils.encodeString("GAAACCTCGTAAGTTTTTTTCTTTCTCTC");
    int pos = m.readBases(0, data, data.length, 1, 10, 0, 1);

    assertEquals(11, pos);
    assertEquals("3.1D7.", m.getCigar(false));
    assertEquals("GAACCTCGTA", DnaUtils.bytesToSequenceIncCG(m.mReadBytes)); //reduced the A homopoly by 1

    String s = m.formatActionsHistogram();
    assertEquals("Total action count:\t11" + StringUtils.LS
        + "Match count:\t10\t90.91%" + StringUtils.LS
        + "Deletion count:\t1\t9.09%" + StringUtils.LS
        + "Total error count:\t1\t9.09%" + StringUtils.LS
        + "Of deletions, due to homopolymer:\t1\t9.09%" + StringUtils.LS, s);
    m.reseedErrorRandom(1348);
    m.resetCigar();
    pos = m.readBases(0, data, data.length, 1, 10, 0, 1);
    assertEquals("4.1I4.1I", m.getCigar(false));
    assertEquals("GAAACCCTCT", DnaUtils.bytesToSequenceIncCG(m.mReadBytes)); //elongated the A homopoly by 1, C by 1 (one of these is NOT due to homopolymer extension!)
    assertEquals(8, pos);
    s = m.formatActionsHistogram();
    assertEquals("Total action count:\t21" + StringUtils.LS
        + "Match count:\t18\t85.71%" + StringUtils.LS
        + "Deletion count:\t1\t4.76%" + StringUtils.LS
        + "Insertion count:\t2\t9.52%" + StringUtils.LS
        + "Total error count:\t3\t14.29%" + StringUtils.LS
        + "Of deletions, due to homopolymer:\t1\t4.76%" + StringUtils.LS
        + "Of insertions, due to homopolymer:\t1\t4.76%" + StringUtils.LS, s);
  }

  public void testEndN() throws Exception {
    final IonTorrentSingleEndMachine m = new IonTorrentSingleEndMachine(4);
    m.reseedErrorRandom(4);
    m.setMinSize(50);
    m.setMaxSize(50);
    m.updateWorkingSpace(10);

    final MockReadWriter mrw = new MockReadWriter();
    m.mReadWriter = mrw;

    final byte[] t = DnaUtils.encodeString("TCAGCTATTGTTCACCTTTCTTCTATACTGTATGTATGTCTCAGCAAGCTTGTGTTTGTTTGGTGGTTGGCTCCTCTATCTGTGGATGCATCAACTCCATNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN");
    m.processFragment("id/", 0, t, 100);
    assertEquals("TGTGTTTGTTTGGTGGTTGGCTCCTCTATCTGTGGATGCATCAACTCCAT", DnaUtils.bytesToSequenceIncCG(mrw.mLastData));
    assertEquals("id/51/F/50.", mrw.mName);
  }

  private static class MockReadWriter implements ReadWriter {
    byte[] mLastData;
    String mName;

    @Override
    public void writeRead(String name, byte[] data, byte[] qual, int length) {
      mLastData = new byte[length];
      System.arraycopy(data, 0, mLastData, 0, length);
      mName = name;
    }

    @Override
    public void close() {
    }

    @Override
    public void identifyTemplateSet(SdfId... templateIds) {
    }

    @Override
    public void identifyOriginalReference(SdfId referenceId) {
    }

    @Override
    public void writeLeftRead(String name, byte[] data, byte[] qual, int length) {
      writeRead(name, data, qual, length);
    }

    @Override
    public void writeRightRead(String name, byte[] data, byte[] qual, int length) {
      writeRead(name, data, qual, length);
    }
    @Override
    public int readsWritten() {
      return 1;
    }
  }
}

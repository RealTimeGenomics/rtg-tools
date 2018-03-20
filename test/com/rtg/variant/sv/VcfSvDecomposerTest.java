/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.variant.sv;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.vcf.ReorderingVcfWriter;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * Tests the corresponding class.
 */
public class VcfSvDecomposerTest extends AbstractCliTest {

  public void testDelSv() {
    final VcfRecord rec = new VcfRecord("1", 2943856, "G").addAltCall("<DEL>");
    rec.addInfo("SVTYPE", "DEL");
    rec.addInfo("END", "39672880");
    rec.addInfo("SVLEN", "-240800");
    rec.addInfo("CIPOS", "-10,15");
    rec.addInfo("CIEND", "-20,25");
    rec.setNumberOfSamples(1);
    rec.addFormatAndSample("GT", "1/1");
    assertEquals("1\t2943857\t.\tG\t<DEL>\t.\t.\tSVTYPE=DEL;END=39672880;SVLEN=-240800;CIPOS=-10,15;CIEND=-20,25\tGT\t1/1", rec.toString());
    final VcfRecord[] out = new VcfSvDecomposer.SvDelDecomposer().decompose(rec);
    assertEquals(2, out.length);
    final Iterator<VcfRecord> it = Arrays.stream(out).sorted(new ReorderingVcfWriter.VcfPositionalComparator()).iterator();
    assertEquals("1\t2943857\t.\tG\tG[1:39672880[\t.\t.\tSVTYPE=BND;CIPOS=-10,15\tGT\t1/1", it.next().toString());
    assertEquals("1\t39672880\t.\tN\t]1:2943857]G\t.\t.\tSVTYPE=BND;CIPOS=-20,25\tGT\t1/1", it.next().toString());
  }

  // Example based on VCF 4.2 spec
  public void testInvSv() {
    final VcfRecord rec = new VcfRecord("2", 321681, "T").addAltCall("<INV>");
    rec.addInfo("SVTYPE", "INV");
    rec.addInfo("END", "421681");
    rec.addInfo("SVLEN", "18028667");
    rec.addInfo("CIPOS", "-10,15");
    rec.addInfo("CIEND", "-20,25");
    assertEquals("2\t321682\t.\tT\t<INV>\t.\t.\tSVTYPE=INV;END=421681;SVLEN=18028667;CIPOS=-10,15;CIEND=-20,25", rec.toString());
    final VcfRecord[] out = new VcfSvDecomposer.SvInvDecomposer().decompose(rec);
    assertEquals(4, out.length);
    final Iterator<VcfRecord> it = Arrays.stream(out).sorted(new ReorderingVcfWriter.VcfPositionalComparator()).iterator();
    assertEquals("2\t321681\t.\tN\tN]2:421681]\t.\t.\tSVTYPE=BND;CIPOS=-10,15", it.next().toString());
    assertEquals("2\t321682\t.\tT\t[2:421682[T\t.\t.\tSVTYPE=BND;CIPOS=-10,15", it.next().toString());
    assertEquals("2\t421681\t.\tN\tN]2:321681]\t.\t.\tSVTYPE=BND;CIPOS=-20,25", it.next().toString());
    assertEquals("2\t421682\t.\tN\t[2:321682[N\t.\t.\tSVTYPE=BND;CIPOS=-20,25", it.next().toString());
  }

  public void testTra() {
    assertEquals("N[1:800[", VcfSvDecomposer.SvTraDecomposer.getBreakpointAlt("N", "1", 799, "3to5").toString());
    assertEquals("]1:800]N", VcfSvDecomposer.SvTraDecomposer.getBreakpointAlt("N", "1", 799, "5to3").toString());
    assertEquals("[1:800[N", VcfSvDecomposer.SvTraDecomposer.getBreakpointAlt("N", "1", 799, "5to5").toString());
    assertEquals("N]1:800]", VcfSvDecomposer.SvTraDecomposer.getBreakpointAlt("N", "1", 799, "3to3").toString());
  }

  public void testDelShort() {
    final Map<String, String> refs = new HashMap<>();
    //             123456 789012345678901234567 8 -- 1-based
    refs.put("1", "CCCCCC GAAAAAAAAAAAAAAAAAAAA T".replaceAll(" ", ""));
    //                                     789012345678901234567
    VcfRecord rec = new VcfRecord("1", 6, "GAAAAAAAAAAAAAAAAAAAA").addAltCall("G");
    rec.setNumberOfSamples(1).addFormatAndSample("GT", "1/1");
    assertEquals("1\t7\t.\tGAAAAAAAAAAAAAAAAAAAA\tG\t.\t.\t.\tGT\t1/1", rec.toString());
    final String hap = "CCCCCC G T".replaceAll(" ", ""); // Expected post-replay haplotype
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));

    final VcfRecord[] out = new VcfSvDecomposer.ShortDelDecomposer().decompose(rec);
    assertEquals(2, out.length);
    final Iterator<VcfRecord> it = Arrays.stream(out).sorted(new ReorderingVcfWriter.VcfPositionalComparator()).iterator();
    rec = it.next();
    assertEquals("1\t7\t.\tG\tG[1:27[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
    rec = it.next();
    assertEquals("1\t27\t.\tA\t]1:7]G\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
  }

  // Same variant as above test, but using <DEL> representation
  public void testDelShortDelRep() {
    final Map<String, String> refs = new HashMap<>();
    //             123456 789012345678901234567 8 -- 1-based
    refs.put("1", "CCCCCC GAAAAAAAAAAAAAAAAAAAA T".replaceAll(" ", ""));
    refs.put("<DEL>", "");
    VcfRecord rec = new VcfRecord("1", 6, "G").addAltCall("<DEL>");
    rec.addInfo("SVTYPE", "DEL");
    rec.addInfo("END", "27");
    rec.addInfo("SVLEN", "-20");
    rec.addInfo("CIPOS", "0,0");
    rec.addInfo("CIEND", "0,0");
    rec.setNumberOfSamples(1).addFormatAndSample("GT", "1/1");
    assertEquals("1\t7\t.\tG\t<DEL>\t.\t.\tSVTYPE=DEL;END=27;SVLEN=-20;CIPOS=0,0;CIEND=0,0\tGT\t1/1", rec.toString());
    final String hap = "CCCCCC G T".replaceAll(" ", ""); // Expected post-replay haplotype
    //assertEquals(hap, VcfUtils.replayAllele(rec, refs));

    final VcfRecord[] out = new VcfSvDecomposer.SvDelDecomposer().decompose(rec);
    assertEquals(2, out.length);
    final Iterator<VcfRecord> it = Arrays.stream(out).sorted(new ReorderingVcfWriter.VcfPositionalComparator()).iterator();
    rec = it.next();
    assertEquals("1\t7\t.\tG\tG[1:27[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
    rec = it.next();
    assertEquals("1\t27\t.\tN\t]1:7]G\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
  }

  public void testInsShort() {
    final Map<String, String> refs = new HashMap<>();
    //                   123456 7 8 -- 1-based
    refs.put("1",       "CCCCCC G N".replaceAll(" ", ""));
    //                   123456 789012345678901234567 8 -- 1-based
    refs.put("<INS_1>", "CCCCCC GAAAAAAAAAAAAAAAAAAAA N".replaceAll(" ", ""));
    VcfRecord rec = new VcfRecord("1", 6, "G").addAltCall("GAAAAAAAAAAAAAAAAAAAA");
    rec.setNumberOfSamples(1).addFormatAndSample("GT", "1/1");
    assertEquals("1\t7\t.\tG\tGAAAAAAAAAAAAAAAAAAAA\t.\t.\t.\tGT\t1/1", rec.toString());
    final String hap = refs.get("<INS_1>"); // Expected post-replay haplotype
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));

    final VcfRecord[] out = new VcfSvDecomposer.ShortInsDecomposer().decompose(rec);
    assertEquals(2, out.length);
    final Iterator<VcfRecord> it = Arrays.stream(out).sorted(new ReorderingVcfWriter.VcfPositionalComparator()).iterator();
    rec = it.next();
    assertEquals("1\t7\t.\tG\tG[<INS_1>:7[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));

    rec = it.next();
    assertEquals("1\t7\t.\tG\t]<INS_1>:27]A\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
  }

  public void testImpureInsShort() {
    final Map<String, String> refs = new HashMap<>();
    //                   123456 789 0 -- 1-based
    refs.put("1",       "CCCCCC TTC N".replaceAll(" ", ""));
    //                   123456 789012345678901234567 8 -- 1-based
    refs.put("<INS_1>", "CCCCCC GGAAAAAAAAAAAAAAAAAAA N".replaceAll(" ", ""));
    VcfRecord rec = new VcfRecord("1", 6, "TTC").addAltCall("GGAAAAAAAAAAAAAAAAAAA");
    rec.setNumberOfSamples(1).addFormatAndSample("GT", "1/1");
    assertEquals("1\t7\t.\tTTC\tGGAAAAAAAAAAAAAAAAAAA\t.\t.\t.\tGT\t1/1", rec.toString());
    final String hap = refs.get("<INS_1>"); // Expected post-replay haplotype
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));

    final VcfRecord[] out = new VcfSvDecomposer.ShortInsDecomposer().decompose(rec);
    assertEquals(2, out.length);
    final Iterator<VcfRecord> it = Arrays.stream(out).sorted(new ReorderingVcfWriter.VcfPositionalComparator()).iterator();
    rec = it.next();
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
    assertEquals("1\t7\t.\tT\tG[<INS_1>:7[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    // Alternative representations
    //assertEquals("1\t7\t.\tTTC\tG[<INS_1>:7[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    //assertEquals("1\t7\t.\tTTC\tGGAAAAAAAAAAAAAAAAAAA[<INS_1>:27[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    //assertEquals("1\t7\t.\tTTC\tGGA[<INS_1>:10[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());

    rec = it.next();
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
    assertEquals("1\t9\t.\tC\t]<INS_1>:27]A\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    //assertEquals("1\t10\t.\tN\t]<INS_1>:28]N\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    // Alternative representations
    //assertEquals("1\t7\t.\tTTCN\t]<INS_1>:7]GGAAAAAAAAAAAAAAAAAAAN\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
  }

  public void testImpureDelShort() {
    final Map<String, String> refs = new HashMap<>();
    //             123456 789012345678901234567 8 -- 1-based
    refs.put("1", "CCCCCC GAAAAAAAAAAAAAAAAAAAA N".replaceAll(" ", ""));
    //                                     789012345678901234567
    VcfRecord rec = new VcfRecord("1", 6, "GAAAAAAAAAAAAAAAAAAAA").addAltCall("TC");
    rec.setNumberOfSamples(1).addFormatAndSample("GT", "1/1");
    assertEquals("1\t7\t.\tGAAAAAAAAAAAAAAAAAAAA\tTC\t.\t.\t.\tGT\t1/1", rec.toString());
    final String hap = "CCCCCC TC N".replaceAll(" ", ""); // Expected post-replay haplotype
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));

    final VcfRecord[] out = new VcfSvDecomposer.ShortDelDecomposer().decompose(rec);
    assertEquals(2, out.length);
    final Iterator<VcfRecord> it = Arrays.stream(out).sorted(new ReorderingVcfWriter.VcfPositionalComparator()).iterator();
    rec = it.next();
    assertEquals("1\t7\t.\tG\tTC[1:27[\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));
    rec = it.next();
    assertEquals("1\t27\t.\tA\t]1:7]TC\t.\t.\tSVTYPE=BND;CIPOS=0,0\tGT\t1/1", rec.toString());
    assertEquals(hap, VcfUtils.replayAllele(rec, refs));

  }

  @Override
  protected AbstractCli getCli() {
    return new VcfSvDecomposer();
  }

  public void testHelp() {
    checkHelp("rtg svdecompose",
      "Split composite structural variants into a breakend representation",
      "minimum length for converting precise insertions and deletions to breakend",
      "VCF file"
    );
  }
}

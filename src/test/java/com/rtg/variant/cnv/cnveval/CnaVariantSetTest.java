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

package com.rtg.variant.cnv.cnveval;

import java.util.Collection;
import java.util.Iterator;

import com.rtg.util.intervals.Range;
import com.rtg.variant.cnv.CnaType;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.eval.VariantSetType;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

public class CnaVariantSetTest extends TestCase {

  public void test() {
    CnaVariantSet s = new CnaVariantSet(new VcfHeader(), VariantSetType.BASELINE);
    final VcfRecord rec1 = CnaVariantTest.makeRecord("chr1", 123, 100123, CnaType.DEL.toString());
    final VcfRecord rec2 = CnaVariantTest.makeRecord("chr2", 123, 100123, CnaType.DUP.toString());
    final VcfRecord rec3 = CnaVariantTest.makeRecord("chr2", 2000000, 3000000, CnaType.DUP.toString());
    s.add(new CnaVariant(new Range(0, 124), rec1));
    s.add(new CnaVariant(new Range(0, 124), rec2));
    s.add(new CnaVariant(new Range(2001000, 2002000), rec3));
    s.add(new CnaVariant(new Range(2003000, 2004000), rec3));
    s.add(new CnaVariant(new Range(2005000, 2006000), rec3));

    s.loaded();

    assertNotNull(s.getHeader());
    assertEquals(2, s.size());

    s.values().stream().flatMap(Collection<CnaVariant>::stream).forEach(c -> c.setCorrect(true));

    s.computeRecordCounts();

    final Collection<CnaRecordStats> rs = s.records();
    assertEquals(3, rs.size());

    final Iterator<CnaRecordStats> i = rs.iterator();
    assertTrue(i.hasNext());
    CnaRecordStats r = i.next();
    assertEquals("chr1", r.record().getSequenceName());
    assertEquals(1, r.hit());
    assertEquals(1.0, r.hitFraction());

    assertTrue(i.hasNext());
    r = i.next();
    assertEquals("chr2", r.record().getSequenceName());
    assertEquals(1, r.hit());

    assertTrue(i.hasNext());
    r = i.next();
    assertEquals("chr2", r.record().getSequenceName());
    assertEquals(3, r.hit());
    assertEquals(1.0, r.hitFraction());

    assertFalse(i.hasNext());
  }
}

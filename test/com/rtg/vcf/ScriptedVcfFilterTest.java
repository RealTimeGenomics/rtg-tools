/*
 * Copyright (c) 2014. Real Time Genomics Limited.
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

package com.rtg.vcf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 *
 */
public class ScriptedVcfFilterTest {

  /** Expected exception handler */
  @Rule
  public ExpectedException mExpectedException = ExpectedException.none();

  private VcfHeader getVcfHeader() {
    final VcfHeader vcfHeader = new VcfHeader();
    vcfHeader.addSampleName("BOB");
    vcfHeader.addFormatField("GT", MetaType.STRING, new VcfNumber("1"), "Genotype");
    vcfHeader.addInfoField(new InfoField("IN", MetaType.STRING, new VcfNumber("1"), "Info field"));
    return vcfHeader;
  }

  private ScriptedVcfFilter getScriptedVcfFilter(String expression, String... begin) {
    return getScriptedVcfFilter(expression, getVcfHeader(), begin);
  }

  private ScriptedVcfFilter getScriptedVcfFilter(String expression, VcfHeader header, String... begin) {
    final ScriptedVcfFilter scriptedVcfFilter = new ScriptedVcfFilter(expression, Arrays.asList(begin), System.err);
    scriptedVcfFilter.setHeader(header);
    return scriptedVcfFilter;
  }

  @Test
  public void testPreambleRef() {
    final ScriptedVcfFilter filter = getScriptedVcfFilter("REF == 'A'");
    assertTrue(filter.accept(new VcfRecord("blah", 1, "A")));
    assertFalse(filter.accept(new VcfRecord("blah", 1, "C")));
  }

  @Test
  public void testPreambleAlts() {
    final VcfRecord record = new VcfRecord("blah", 1, "A");
    record.setNumberOfSamples(1);
    record.addAltCall("G");
    record.addAltCall("T");
    assertTrue(getScriptedVcfFilter("ALT.indexOf('T') > -1").accept(record));
    assertFalse(getScriptedVcfFilter("ALT.indexOf('C') > -1").accept(record));
    assertTrue(getScriptedVcfFilter("ALT.length == 2").accept(record));
  }

  @Test
  public void testSampleFormats() {
    final VcfRecord record = new VcfRecord("blah", 1, "A");
    record.addAltCall("G");
    record.setNumberOfSamples(1);
    record.addFormatAndSample("GT", "0/1");
    assertTrue(getScriptedVcfFilter("BOB.GT == '0/1'").accept(record));
    assertFalse(getScriptedVcfFilter("BOB.GT == '1/0'").accept(record));
    mExpectedException.expect(NoTalkbackSlimException.class);
    getScriptedVcfFilter("FRANK.GT == '1/0'").accept(record);
  }

  @Test
  public void testWeirdSampleFormats() {
    final VcfHeader header = getVcfHeader();
    header.addSampleName("FRANK-2");
    final VcfRecord record = new VcfRecord("blah", 1, "A");
    record.addAltCall("G");
    record.setNumberOfSamples(2);
    record.addFormatAndSample("GT", "0/1");
    record.addFormatAndSample("GT", "1/1");
    assertTrue(getScriptedVcfFilter("BOB.GT == '0/1'", header).accept(record));
    // Via string prototype
    assertTrue(getScriptedVcfFilter("'FRANK-2'.GT == '1/1'", header).accept(record));
  }

  @Test
  public void testMissing() {
    final VcfRecord record = new VcfRecord("blah", 1, "A");
    record.addAltCall("G");
    assertFalse(getScriptedVcfFilter("BOB.GT == '0/1'").accept(record));
    assertTrue(getScriptedVcfFilter("BOB.GT == '.'").accept(record));
  }

  @Test
  public void testChrom() {
    // Pos is one based
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    assertFalse(getScriptedVcfFilter("CHROM == 'chrom'").accept(record));
    assertTrue(getScriptedVcfFilter("CHROM == 'blah'").accept(record));
  }
  @Test
  public void testPos() {
    // Pos is one based
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    assertFalse(getScriptedVcfFilter("POS == 0").accept(record));
    assertTrue(getScriptedVcfFilter("POS == 1").accept(record));
    assertFalse(getScriptedVcfFilter("POS == 2").accept(record));
  }
  @Test
  public void testOrder() {
    VcfHeader header = getVcfHeader();
    header.addFormatField("DP", MetaType.INTEGER, VcfNumber.ONE, "FOO");
    final ScriptedVcfFilter filter = getScriptedVcfFilter("SAMPLES[0].DP >= 10", header, "function record() {SAMPLES[0].DP *= 4; return true;}");
    final VcfRecord recordA = new VcfRecord("blah", 2, "A");
    recordA.setNumberOfSamples(1);
    recordA.addFormatAndSample("DP", "10");
    final VcfRecord recordB = new VcfRecord("blah", 4, "A");
    recordB.setNumberOfSamples(1);
    recordB.addFormatAndSample("DP", "9");
    assertTrue(filter.accept(recordA));
    assertFalse(filter.accept(recordB));
  }

  @Test
  public void testQual() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setQuality("100");
    assertFalse(getScriptedVcfFilter("QUAL == 10").accept(record));
    assertTrue(getScriptedVcfFilter("QUAL == 100").accept(record));
  }
  @Test
  public void testId() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setId("VAR1");
    assertFalse(getScriptedVcfFilter("ID == 'VAR2'").accept(record));
    assertTrue(getScriptedVcfFilter("ID == 'VAR1'").accept(record));
  }

  @Test
  public void testFilter() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addFilter("FAIL");
    assertFalse(getScriptedVcfFilter("FILTER.indexOf('PASS') > -1").accept(record));
    assertTrue(getScriptedVcfFilter("FILTER.indexOf('FAIL') > -1").accept(record));
  }

  @Test
  public void testInfo() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertFalse(getScriptedVcfFilter("INFO.IN == 'BAR'").accept(record));
    assertTrue(getScriptedVcfFilter("INFO.IN == 'FOO'").accept(record));
  }
  @Test
  public void testMissingInfo() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    assertTrue(getScriptedVcfFilter("INFO.IN == '.'").accept(record));
  }

  @Test
  public void testStringFilter() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addFormatAndSample("GT", "0/1");
    assertTrue(getScriptedVcfFilter("'BOB'.GT == '0/1'").accept(record));
  }

  @Test
  public void testComplexExpression() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setNumberOfSamples(1);
    record.addInfo("IN", "FOO");
    record.addFormatAndSample("GT", "0/1");
    // Check multiple js expressions variable assignment etc
    assertTrue(getScriptedVcfFilter("gt = 'BOB'.GT; foo = INFO.IN == 'FOO'; foo && REF == 'A' && gt == '0/1'").accept(record));
    assertFalse(getScriptedVcfFilter("gt = 'BOB'.GT; foo = INFO.IN == 'FOO'; !foo && REF == 'A' && gt == '0/1'").accept(record));
  }

  @Test
  public void testUpdateFormat() {
      final VcfRecord record = new VcfRecord("blah", 1, "A");
      record.addAltCall("G");
      record.setNumberOfSamples(1);
      record.addFormatAndSample("GT", "0/1");
      assertTrue(getScriptedVcfFilter("BOB.GT = '0/0'; true").accept(record));
      assertEquals("0/0", record.getFormatAndSample().get("GT").get(0));
  }
  @Test
  public void testUpdateFormatViaString() {
    final VcfRecord record = new VcfRecord("blah", 1, "A");
    record.addAltCall("G");
    record.setNumberOfSamples(1);
    record.addFormatAndSample("GT", "0/1");
    assertTrue(getScriptedVcfFilter("'BOB'.GT = '0/0'; true").accept(record));
    assertEquals("0/0", record.getFormatAndSample().get("GT").get(0));
  }

  @Test
  public void testUpdateInfo() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = 'BAZ,BANG'; true").accept(record));
    assertEquals(Arrays.asList("BAZ", "BANG"), record.getInfo().get("IN"));
  }

  @Test
  public void testUpdateInfoNumerics() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = '12,-99'; true").accept(record));
    assertEquals(Arrays.asList("12", "-99"), record.getInfo().get("IN"));
  }

  @Test
  public void testUpdateInfoSingle() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = 'BAZ'; true").accept(record));
    assertEquals(Collections.singletonList("BAZ"), record.getInfo().get("IN"));
  }

  @Test
  public void testUpdateInfoSingleNumeric() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = -99; true").accept(record));
    assertEquals(Collections.singletonList("-99"), record.getInfo().get("IN"));
  }

  @Test
  public void testUpdateInfoFlag() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN");
    assertTrue(record.getInfo().containsKey("IN"));
    assertTrue(getScriptedVcfFilter("INFO.IN = false; true").accept(record));
    assertFalse(record.getInfo().containsKey("IN"));
    assertTrue(getScriptedVcfFilter("INFO.IN = true; true").accept(record));
    assertTrue(record.getInfo().containsKey("IN"));
  }

  @Test
  public void testClearInfo() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");

    record.addInfo("IN");
    assertTrue(getScriptedVcfFilter("INFO.IN = null; true").accept(record));
    assertFalse(record.getInfo().containsKey("IN"));

    record.addInfo("IN");
    assertTrue(getScriptedVcfFilter("INFO.IN = '.'; true").accept(record));
    assertFalse(record.getInfo().containsKey("IN"));
  }

  @Test
  public void testRecordFunctionNoReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter(null, "function record() {}").accept(record));
  }

  @Test
  public void testRecordFunctionInvalidReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    mExpectedException.expect(NoTalkbackSlimException.class);
    mExpectedException.expectMessage(StringContains.containsString("The return value of the record function was not a boolean."));
    assertTrue(getScriptedVcfFilter(null, "function record() {return 'false';}").accept(record));
  }

  @Test
  public void testRecordFunctionAcceptingReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter(null, "function record() {return true;}").accept(record));
  }

  @Test
  public void testRecordFunctionRejectingReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertFalse(getScriptedVcfFilter(null, "function record() {return false;}").accept(record));
  }

  @Test
  public void testMultipleBegins() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter(null, "var expected = 'A'", "function record() {return REF == expected;}").accept(record));
  }
}
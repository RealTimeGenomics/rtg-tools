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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.FilterField;
import com.rtg.vcf.header.FormatField;
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
  public void testVersionExists() {
    final VcfRecord record = new VcfRecord("blah", 1, "A");
    assertTrue(getScriptedVcfFilter("typeof NO_RTG_VERSION == 'undefined'").accept(record));
    assertTrue(getScriptedVcfFilter("typeof RTG_VERSION != 'undefined'").accept(record));
  }

  private void expectThrows(String expr) {
    try {
      getScriptedVcfFilter("true;", expr);
      fail();
    } catch (NoTalkbackSlimException e) {
      // Expected
      //System.err.println(e.getMessage());
    }
  }

  @Test
  public void testMinVersion() {
    expectThrows("preMinVersionExistence('3.9.2');");
    expectThrows("RTG_VERSION = '3.9.1'; checkMinVersion('3.9.2')");
    expectThrows("RTG_VERSION = '3.9-dev'; checkMinVersion('3.9.2')");
    expectThrows("RTG_VERSION = '4.0.1'; checkMinVersion('2017-10-23')");
    expectThrows("RTG_VERSION = '4.0.1'; checkMinVersion('a.b.c')");
    expectThrows("RTG_VERSION = '4.0.1'; checkMinVersion('foo')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '3.9.2'; checkMinVersion('3.9')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '3.9-dev'; checkMinVersion('3.9')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '3.9.2'; checkMinVersion('3.9.2')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '3.10'; checkMinVersion('3.9.2')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '3.10.1'; checkMinVersion('3.9.2')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '4.0'; checkMinVersion('3.9.2')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '4.0.1'; checkMinVersion('3.9.2')");
    getScriptedVcfFilter("true;", "RTG_VERSION = '2017-11-12'; checkMinVersion('3.9.2')");
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
    final VcfHeader header = getVcfHeader();
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
  public void testAddHeaders() {
    final VcfHeader header = getVcfHeader();
    final FilterField filterField = new FilterField("newFilter", "FOO");
    final InfoField infoField = new InfoField("newInfo", MetaType.INTEGER, VcfNumber.ONE, "FOO");
    final FormatField formatField = new FormatField("newFormat", MetaType.INTEGER, VcfNumber.ONE, "FOO");
    getScriptedVcfFilter("true", header,
      "ensureFilterHeader('" + filterField.toString() + "');",
      "ensureInfoHeader('" + infoField.toString() + "');",
      "ensureFormatHeader('" + formatField.toString() + "');"
    );
    assertNotNull(header.getFilterField(filterField.getId()));
    assertNotNull(header.getInfoField(infoField.getId()));
    assertNotNull(header.getFormatField(formatField.getId()));
  }

  @Test
  public void testQual() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    assertTrue(getScriptedVcfFilter("QUAL == '.'").accept(record));
    record.setQuality("100");
    assertFalse(getScriptedVcfFilter("QUAL == 10").accept(record));
    assertTrue(getScriptedVcfFilter("QUAL == 100").accept(record));
  }
  @Test
  public void testId() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    assertTrue(getScriptedVcfFilter("ID == '.'").accept(record));
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
    record.setInfo("IN", "FOO");
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
  public void testBadExpression() {
    try {
      // That's a user-typo'ed greater-than-or-equals which is a bogus javascript arrow function expression that gave a new flavour of parsing exception
      getScriptedVcfFilter("'BOB'.GT => 30");
      fail("Expected JVM javascript engine to choke while parsing invalid expression");
    } catch (NoTalkbackSlimException e) {
      // Expected
    }
  }

  @Test
  public void testComplexExpression() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setNumberOfSamples(1);
    record.setInfo("IN", "FOO");
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
  public void testUpdateChrom() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setSequenceName("foo");
    assertEquals("foo", record.getSequenceName());
    assertTrue(getScriptedVcfFilter("CHROM = 'bar'; true").accept(record));
    assertEquals("bar", record.getSequenceName());
  }

  @Test
  public void testUpdatePos() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setStart(10);
    assertEquals(10, record.getStart());
    assertTrue(getScriptedVcfFilter("POS = 2; true").accept(record));
    assertEquals(1, record.getStart());
    assertTrue(getScriptedVcfFilter("POS = POS + 3; true").accept(record));
    assertEquals(4, record.getStart());
  }

  @Test
  public void testUpdateId() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    assertEquals(VcfRecord.MISSING, record.getId());
    // Set via string
    assertTrue(getScriptedVcfFilter("ID = 'BAZ;BANG'; true").accept(record));
    assertEquals("BAZ;BANG", record.getId());
    assertTrue(getScriptedVcfFilter("ID = '.'; true").accept(record));
    assertEquals(VcfRecord.MISSING, record.getId());
    assertTrue(getScriptedVcfFilter("ID = ''; true").accept(record));
    assertEquals(VcfRecord.MISSING, record.getId());
    assertTrue(getScriptedVcfFilter("ID = '0'; true").accept(record));
    assertEquals("0", record.getId());
    // Set via array
    assertTrue(getScriptedVcfFilter("ID = ['BAZ', 'BANG']; true").accept(record));
    assertEquals("BAZ;BANG", record.getId());
  }

  @Test
  public void testUpdateQual() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setQuality("0.5");
    assertEquals("0.5", record.getQuality());
    assertTrue(getScriptedVcfFilter("QUAL = '0.9'; true").accept(record));
    assertEquals("0.9", record.getQuality());
  }

  @Test
  public void testUpdateFilter() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.addFilter("PASS");
    assertFalse(record.isFiltered());
    // Set via string
    assertTrue(getScriptedVcfFilter("FILTER = 'BAZ;BANG'; true").accept(record));
    assertTrue(record.isFiltered());
    assertTrue(record.getFilters().contains("BAZ"));
    assertTrue(record.getFilters().contains("BANG"));
    // Set via array
    assertTrue(getScriptedVcfFilter("FILTER = 'BAZ;BANG;BOING'.split(';'); true").accept(record));
    // Append via string (ugly)
    assertTrue(getScriptedVcfFilter("FILTER = FILTER.join(';') + ';BOUNCE'; true").accept(record));
    // Append via array append through var (ugly)
    assertTrue(getScriptedVcfFilter("f = FILTER; f.push('BOINK'); FILTER = f; true").accept(record));
    // Append via special mutator (not as ugly)
    assertTrue(getScriptedVcfFilter("FILTER.add('NOINK').add('DOINK'); true").accept(record));
    assertTrue(record.isFiltered());
    //System.err.println(record.getFilters());
    assertTrue(record.getFilters().contains("BAZ"));
    assertTrue(record.getFilters().contains("BANG"));
    assertTrue(record.getFilters().contains("BOING"));
    assertTrue(record.getFilters().contains("BOUNCE"));
    assertTrue(record.getFilters().contains("BOINK"));
    assertTrue(record.getFilters().contains("NOINK"));
    assertTrue(record.getFilters().contains("DOINK"));
    assertTrue(getScriptedVcfFilter("FILTER.add('0'); true").accept(record));
    assertTrue(record.isFiltered());
    assertTrue(getScriptedVcfFilter("FILTER = '.'; true").accept(record));
    assertFalse(record.isFiltered());
    assertTrue(getScriptedVcfFilter("FILTER = ['BAZ', 'BANG']; true").accept(record));
    assertTrue(record.isFiltered());
    assertTrue(record.getFilters().contains("BAZ"));
    assertTrue(record.getFilters().contains("BANG"));
    assertTrue(getScriptedVcfFilter("FILTER = ''; true").accept(record));
    assertFalse(record.isFiltered());
    assertTrue(getScriptedVcfFilter("FILTER = '0'; true").accept(record));
    assertTrue(record.isFiltered());
    assertTrue(record.getFilters().contains("0"));
  }

  @Test
  public void testUpdateInfo() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = 'BAZ,BANG'; true").accept(record));
    assertEquals(Arrays.asList("BAZ", "BANG"), Arrays.asList(record.getInfoSplit("IN")));
  }

  @Test
  public void testUpdateInfoNumerics() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = '12,-99'; true").accept(record));
    assertEquals(Arrays.asList("12", "-99"), Arrays.asList(record.getInfoSplit("IN")));
    assertTrue(getScriptedVcfFilter("INFO.IN = '0,1'; true").accept(record));
    assertEquals(Arrays.asList("0", "1"), Arrays.asList(record.getInfoSplit("IN")));
  }

  @Test
  public void testUpdateInfoSingle() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = 'BAZ'; true").accept(record));
    assertEquals("BAZ", record.getInfo("IN"));
  }

  @Test
  public void testUpdateInfoSingleNumeric() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter("INFO.IN = -99; true").accept(record));
    assertEquals("-99", record.getInfo("IN"));
    assertTrue(getScriptedVcfFilter("INFO.IN = 1; true").accept(record));
    assertEquals("1", record.getInfo("IN"));
    assertTrue(getScriptedVcfFilter("INFO.IN = 0; true").accept(record));
    assertEquals("0", record.getInfo("IN"));
  }

  @Test
  public void testUpdateInfoFlag() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN");
    assertTrue(record.hasInfo("IN"));
    assertTrue(getScriptedVcfFilter("INFO.IN = false; true").accept(record));
    assertFalse(record.hasInfo("IN"));
    assertTrue(getScriptedVcfFilter("INFO.IN = true; true").accept(record));
    assertTrue(record.hasInfo("IN"));
  }

  @Test
  public void testClearInfo() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");

    record.setInfo("IN");
    assertTrue(getScriptedVcfFilter("INFO.IN = null; true").accept(record));
    assertFalse(record.hasInfo("IN"));

    record.setInfo("IN");
    assertTrue(getScriptedVcfFilter("INFO.IN = '.'; true").accept(record));
    assertFalse(record.hasInfo("IN"));

    record.setInfo("IN");
    assertTrue(getScriptedVcfFilter("INFO.IN = ''; true").accept(record));
    assertFalse(record.hasInfo("IN"));
  }

  @Test
  public void testRecordFunctionNoReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter(null, "function record() {}").accept(record));
  }

  @Test
  public void testRecordFunctionInvalidReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    mExpectedException.expect(NoTalkbackSlimException.class);
    mExpectedException.expectMessage(StringContains.containsString("The return value of the record function was not a boolean."));
    assertTrue(getScriptedVcfFilter(null, "function record() {return 'false';}").accept(record));
  }

  @Test
  public void testRecordFunctionAcceptingReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter(null, "function record() {return true;}").accept(record));
  }

  @Test
  public void testRecordFunctionRejectingReturn() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertFalse(getScriptedVcfFilter(null, "function record() {return false;}").accept(record));
  }

  @Test
  public void testMultipleBegins() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "FOO");
    assertTrue(getScriptedVcfFilter(null, "var expected = 'A'", "function record() {return REF == expected;}").accept(record));
  }

  @Test
  public void testInfoMatch() {
    final VcfRecord record = new VcfRecord("blah", 0, "A");
    record.setInfo("IN", "T|synonymous_variant,T|intron_variant&non_coding_transcript_variant|MODIFIER|||||||||");
    assertTrue(record.hasInfo("IN"));
    assertTrue(getScriptedVcfFilter("has(INFO.IN)").accept(record));
    assertTrue(getScriptedVcfFilter("INFO.IN.indexOf(\"intron_variant\") != -1").accept(record));
    assertTrue(getScriptedVcfFilter("INFO.IN.match(\"intron_variant\") != null").accept(record));
  }

}

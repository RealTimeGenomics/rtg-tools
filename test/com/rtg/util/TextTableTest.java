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
package com.rtg.util;

import java.io.IOException;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.SlimException;

/**
 */
public class TextTableTest extends AbstractNanoTest {

  public void testDefault() throws IOException {
    final TextTable table = new TextTable();
    assertEquals("", table.toString());
    assertEquals(0, table.numRows());

    table.addHeaderRow("Depth", "Breadth", "Covered", "Size", "Non-N Depth", "Non-N Breadth", "Non-N Covered", "Non-N Size", "Name");
    table.addSeparator();
    table.addRow("0.0097", "0.0087", "2140198", "247249719", "0.0107", "0.0095", "2140198", "224999719", "chr1");
    table.addRow("0.0112", "0.0093", "1259191", "135374737", "0.0116", "0.0096", "1259191", "131624728", "chr10");
    table.addRow("0.0100", "0.0093", "1256531", "134452384", "0.0102", "0.0096", "1256531", "131130753", "chr11");
    table.addRow("0.0093", "0.0088", "1162746", "132349534", "0.0095", "0.0089", "1162746", "130303032", "chr12");
    assertEquals(6, table.numRows());

    mNano.check("tt-default.txt", table.toString());
    mNano.check("tt-default.tsv", table.getAsTsv());

    table.setAlignment(TextTable.Align.LEFT, TextTable.Align.RIGHT, TextTable.Align.CENTER, TextTable.Align.LEFT, TextTable.Align.CENTER, TextTable.Align.RIGHT, TextTable.Align.LEFT);
    mNano.check("tt-mixed-alignments.txt", table.toString());
  }

  public void testIndent() throws IOException {
    final TextTable table = new TextTable(3, 2, TextTable.Align.RIGHT);
    table.addRow("1:", "4");
    table.addRow("2:", "42");
    table.addRow("10:", "24");
    table.addRow("longer:", "333");

    mNano.check("tt-indent.txt", table.toString());

    table.setAlignment(TextTable.Align.LEFT);
    mNano.check("tt-indent-1left.txt", table.toString());

    table.setAlignment(TextTable.Align.CENTER);
    mNano.check("tt-indent-1center.txt", table.toString());
  }

  public void testSingleRowAndColumn() {
    final TextTable table = new TextTable();
    table.addRow("1");
    assertEquals("1" + StringUtils.LS, table.toString());
  }

  public void testErrors() {
    Diagnostic.setLogStream();
    final TextTable table = new TextTable();
    table.addRow("1", "2");
    try {
      table.addRow("1", "2", "3");
      fail();
    } catch (SlimException e) {
      assertEquals("Mismatching number of columns in table formatter", e.getMessage());
    }
    try {
      table.addRow("1", null);
      fail();
    } catch (SlimException e) {
      assertEquals("Null provided as value in table formatters", e.getMessage());
    }
  }
}

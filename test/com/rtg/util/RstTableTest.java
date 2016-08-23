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

import junit.framework.TestCase;

/**
 *
 */
public class RstTableTest extends TestCase {

  public void testEscape() {
    assertEquals("rolling\\_pin", RstTable.escapeText("rolling_pin"));
    assertEquals("rolling\\\\pin", RstTable.escapeText("rolling\\pin"));
  }

  private static final String EXP = ""
    + "+-------+-----------------+---------+" + StringUtils.LS
    + "| title text                        |" + StringUtils.LS
    + "+=======+=================+=========+" + StringUtils.LS
    + "| lala  | elaboration     | eh      |" + StringUtils.LS
    + "+-------+-----------------+---------+" + StringUtils.LS
    + "| hmm   | longer          | short   |" + StringUtils.LS
    + "+-------+-----------------+---------+" + StringUtils.LS;

  public void test() {
    final RstTable table = new RstTable(1, 10, 5, 15, 7);
    table.addHeading("title text");
    table.addRow("lala", "elaboration", "eh");
    table.addRow("hmm", "longer", "short");
    assertEquals(EXP, table.getText());
  }
}
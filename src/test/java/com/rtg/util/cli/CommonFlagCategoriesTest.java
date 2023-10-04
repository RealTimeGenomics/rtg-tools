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
package com.rtg.util.cli;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;


/**
 */
public class CommonFlagCategoriesTest extends TestCase {

  public void testSetCategories() {
    final CFlags flags = new CFlags();
    flags.registerOptional("a", "a").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    flags.registerOptional("b", "b").setCategory(CommonFlagCategories.REPORTING);
    flags.registerOptional("c", "c").setCategory(CommonFlagCategories.SENSITIVITY_TUNING);
    flags.registerOptional("d", "d").setCategory(CommonFlagCategories.UTILITY);
    flags.registerOptional("e", "e").setCategory(CommonFlagCategories.FILTERING);
    CommonFlagCategories.setCategories(flags);
    assertEquals("Utility", flags.getFlag("help").getCategory());
    TestUtils.containsAll(flags.getUsageString(), "File Input/Output", "Reporting", "Sensitivity Tuning", "Utility");
  }
}

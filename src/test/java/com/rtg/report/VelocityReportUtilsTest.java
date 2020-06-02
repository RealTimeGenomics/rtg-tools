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
package com.rtg.report;

import java.io.IOException;
import java.util.HashMap;

import com.rtg.util.HtmlReportHelper;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class VelocityReportUtilsTest extends TestCase {

  public void testTemplate() throws IOException {
    final HashMap<String, String> map = new HashMap<>();
    map.put("aVariable", "foorah");
    map.put("bVariable", "inner stuff");
    final String res = VelocityReportUtils.processTemplate("velocityTest.txt", map);
    assertEquals("foorah\ntextNextToinner stufffoo\n\n", res);
    map.put("valExists", "all new value");
    final String res2 = VelocityReportUtils.processTemplate("velocityTest.txt", map);
    assertEquals("foorah\ntextNextToinner stufffoo\n\nall new value\n", res2);
  }

  public void testWrapDefault() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final HtmlReportHelper hrh = new HtmlReportHelper(dir, "index");
      final String res = VelocityReportUtils.wrapDefaultTemplate("hardbody", "test", hrh);

      TestUtils.containsAll(res, "\nhardbody\n    </div>",
                                   "title>test</title",
                                   "href=\"" + hrh.getResourcesDirName() + "/rtg.css");
    }
  }
}

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
import java.io.InputStream;

import com.rtg.util.io.IOUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for <code>Resources</code>.
 *
 */
public class ResourcesTest extends TestCase {

  /**
   * Constructor (needed for JUnit)
   *
   * @param name A string which names the tests.
   *
   */
  public ResourcesTest(final String name) {
    super(name);
  }

  public void testResources() throws IOException {
    InputStream i = Resources.getResourceAsStream("com/rtg/util/resources/ethwinout.txt");
    try {
      IOUtils.readAll(i);
    } finally {
      i.close();
    }
    i = Resources.getResourceAsStream(getClass(), "com/rtg/util/resources/ethwinout.txt");
    try {
      IOUtils.readAll(i);
    } finally {
      i.close();
    }

  }

  public void testSlash() {
    assertEquals("com/rtg/", Resources.trailingSlash("com/rtg", true));
    assertEquals("com/rtg", Resources.trailingSlash("com/rtg", false));
    assertEquals("com/rtg/", Resources.trailingSlash("com/rtg/", true));
    assertEquals("com/rtg", Resources.trailingSlash("com/rtg/", false));
  }
//
//  Unfortunately most of our packages exist in multiple heirachies, making output from listResources non-fixed
//  public void testList(), URISyntaxException {
//    checkList("com/rtg/util");
//    checkList("com/rtg/util/");
//
//  }
//
//  private void checkList(String resource), IOException {
//    String[] res = Resources.listResources(resource);
//    boolean found = false;
//    for (String f : res) {
//      if (f.endsWith("com/rtg/util/ethwinout.txt")) {
//        found = true;
//      }
//    }
//    assertTrue(found);
//  }

  public static Test suite() {
    return new TestSuite(ResourcesTest.class);
  }

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}


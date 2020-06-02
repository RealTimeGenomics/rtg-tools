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

package com.rtg.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;

import junit.framework.TestCase;


/**
 * Tests for Property Utility class.
 */
public class PropertiesUtilsTest extends TestCase {

  public void testGetPropertiesResource() throws InvalidParamsException, IOException {
    Diagnostic.setLogStream();
    final Properties pr = PropertiesUtils.getPriorsResource("human", PropertiesUtils.PropertyType.PRIOR_PROPERTY);
    assertNotNull(pr);
    final ByteArrayOutputStream log = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(log);
    Diagnostic.setLogStream(ps);
    try {
      PropertiesUtils.getPriorsResource("non-existant", PropertiesUtils.PropertyType.PRIOR_PROPERTY);
      fail();
    } catch (InvalidParamsException e) {
      assertEquals(ErrorType.INFO_ERROR, e.getErrorType());
      assertTrue(e.getMessage().contains("Invalid prior option \"non-existant"));
//      ps.flush();
//      TestUtils.containsAll(log.toString(), "non-existant", /*"as a properties file for priors"*/ "Invalid prior option");
    } finally {
      Diagnostic.setLogStream();
    }
  }

//  public void testGetIntegerProperty() {
//    int x = PropertiesUtils.getIntegerProperty("propertyutils.blah", 123);
//    assertEquals(123, x);
//
//    //%System.Environment.SetEnvironmentVariable("propertyutils.blah", "2345");
//    System.setProperty("propertyutils.blah", "2345");
//    x = PropertiesUtils.getIntegerProperty("propertyutils.blah", 123);
//    assertEquals(2345, x);
//
//    //%System.Environment.SetEnvironmentVariable("propertyutils.blah", "abc");
//    System.setProperty("propertyutils.blah", "abc");
//    try {
//      PropertiesUtils.getIntegerProperty("propertyutils.blah", 123);
//      fail("Expected an Exception");
//    } catch (RuntimeException e) {
//      //e.printStackTrace();
//    }
//  }
}

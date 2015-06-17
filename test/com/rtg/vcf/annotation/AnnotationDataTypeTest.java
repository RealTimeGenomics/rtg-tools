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

package com.rtg.vcf.annotation;

import com.rtg.util.TestUtils;
import com.rtg.vcf.header.MetaType;

import junit.framework.TestCase;

/**
 */
public class AnnotationDataTypeTest extends TestCase {

  public void testEnum() {
    TestUtils.testEnum(AnnotationDataType.class, "[BOOLEAN, INTEGER, DOUBLE, STRING]");
    assertEquals(0, AnnotationDataType.BOOLEAN.ordinal());
    assertEquals(1, AnnotationDataType.INTEGER.ordinal());
    assertEquals(2, AnnotationDataType.DOUBLE.ordinal());
    assertEquals(3, AnnotationDataType.STRING.ordinal());

    assertEquals(4, AnnotationDataType.values().length);
  }

  public void testGetClassType() {
    assertEquals(String.class, AnnotationDataType.STRING.getClassType());
    assertEquals(Double.class, AnnotationDataType.DOUBLE.getClassType());
    assertEquals(Boolean.class, AnnotationDataType.BOOLEAN.getClassType());
    assertEquals(Integer.class, AnnotationDataType.INTEGER.getClassType());
  }

  public void testIsMetaTypeCompatiable() {
    assertTrue(AnnotationDataType.STRING.isMetaTypeCompatible(MetaType.STRING));
    assertTrue(AnnotationDataType.STRING.isMetaTypeCompatible(MetaType.CHARACTER));
    assertFalse(AnnotationDataType.STRING.isMetaTypeCompatible(MetaType.INTEGER));
    assertFalse(AnnotationDataType.STRING.isMetaTypeCompatible(MetaType.FLAG));
    assertFalse(AnnotationDataType.STRING.isMetaTypeCompatible(MetaType.FLOAT));

    assertTrue(AnnotationDataType.INTEGER.isMetaTypeCompatible(MetaType.INTEGER));
    assertFalse(AnnotationDataType.INTEGER.isMetaTypeCompatible(MetaType.CHARACTER));
    assertFalse(AnnotationDataType.INTEGER.isMetaTypeCompatible(MetaType.STRING));
    assertFalse(AnnotationDataType.INTEGER.isMetaTypeCompatible(MetaType.FLAG));
    assertFalse(AnnotationDataType.INTEGER.isMetaTypeCompatible(MetaType.FLOAT));

    assertTrue(AnnotationDataType.DOUBLE.isMetaTypeCompatible(MetaType.FLOAT));
    assertFalse(AnnotationDataType.DOUBLE.isMetaTypeCompatible(MetaType.CHARACTER));
    assertFalse(AnnotationDataType.DOUBLE.isMetaTypeCompatible(MetaType.INTEGER));
    assertFalse(AnnotationDataType.DOUBLE.isMetaTypeCompatible(MetaType.FLAG));
    assertFalse(AnnotationDataType.DOUBLE.isMetaTypeCompatible(MetaType.STRING));

    assertTrue(AnnotationDataType.BOOLEAN.isMetaTypeCompatible(MetaType.FLAG));
    assertFalse(AnnotationDataType.BOOLEAN.isMetaTypeCompatible(MetaType.CHARACTER));
    assertFalse(AnnotationDataType.BOOLEAN.isMetaTypeCompatible(MetaType.INTEGER));
    assertFalse(AnnotationDataType.BOOLEAN.isMetaTypeCompatible(MetaType.STRING));
    assertFalse(AnnotationDataType.BOOLEAN.isMetaTypeCompatible(MetaType.FLOAT));
  }

  public void testConvert() {
    for (AnnotationDataType adt : AnnotationDataType.values()) {
      assertNull(adt.stringToObjectOfType(null));
      if (adt != AnnotationDataType.STRING && adt != AnnotationDataType.BOOLEAN) {
        try {
          adt.stringToObjectOfType("adsafsfd");
          fail(adt + " formatted bad value");
        } catch (IllegalArgumentException iae) {
          // expected
        }
      }
    }
    Object o = AnnotationDataType.STRING.stringToObjectOfType("banana");
    assertTrue(o instanceof String);
    assertEquals("banana", o);

    o = AnnotationDataType.INTEGER.stringToObjectOfType("123");
    assertTrue(o instanceof Integer);
    assertEquals(123, o);

    o = AnnotationDataType.DOUBLE.stringToObjectOfType("123");
    assertTrue(o instanceof Double);
    assertEquals(123.0, o);

    o = AnnotationDataType.BOOLEAN.stringToObjectOfType("true");
    assertTrue(o instanceof Boolean);
    assertEquals(Boolean.TRUE, o);

  }
}

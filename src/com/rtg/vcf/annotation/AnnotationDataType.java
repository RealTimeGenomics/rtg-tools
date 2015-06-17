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

import com.rtg.vcf.header.MetaType;

/**
 * Data type to allow conversion between AVR types and other types.
 */
public enum AnnotationDataType {
  /**
   * Boolean/flag type.
   */
  BOOLEAN {
    @Override
    public Class<?> getClassType() {
      return Boolean.class;
    }

    @Override
    public Object stringToObjectOfType(String str) {
      return str == null ? null : Boolean.valueOf(str);
    }

    @Override
    public boolean isMetaTypeCompatible(MetaType mt) {
      return mt == MetaType.FLAG;
    }
  },
  /**
   * Integer type.
   */
  INTEGER {
    @Override
    public Class<?> getClassType() {
      return Integer.class;
    }

    @Override
    public Object stringToObjectOfType(String str) {
      return str == null ? null : Integer.valueOf(str);
    }

    @Override
    public boolean isMetaTypeCompatible(MetaType mt) {
      return mt == MetaType.INTEGER;
    }
  },
  /**
   * Double/float type.
   */
  DOUBLE {
    @Override
    public Class<?> getClassType() {
      return Double.class;
    }

    @Override
    public Object stringToObjectOfType(String str) {
      return str == null ? null : Double.valueOf(str);
    }

    @Override
    public boolean isMetaTypeCompatible(MetaType mt) {
      return mt == MetaType.FLOAT;
    }
  },
  /**
   * Generic string type.
   */
  STRING {
    @Override
    public Class<?> getClassType() {
      return String.class;
    }

    @Override
    public Object stringToObjectOfType(String str) {
      return str;
    }

    @Override
    public boolean isMetaTypeCompatible(MetaType mt) {
      return mt == MetaType.STRING || mt == MetaType.CHARACTER;
    }
  };

  /**
   * Return the java class type associated with the {@link AnnotationDataType}.
   * @return java class
   */
  public abstract Class<?> getClassType();

  /**
   * Return a type specific conversion of the given string as an object.
   * @param str string to convert
   * @return object version of string
   * @throws IllegalArgumentException if string cannot be converted
   */
  public abstract Object stringToObjectOfType(String str);

  /**
   * Test whether this {@link AnnotationDataType} is compatible with the given {@link MetaType}.
   * @param mt {@link MetaType} to test
   * @return true if types are compatible
   */
  public abstract boolean isMetaTypeCompatible(MetaType mt);
}

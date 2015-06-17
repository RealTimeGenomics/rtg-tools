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

import java.util.HashSet;
import java.util.Set;

import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Abstract class to use when implementing a derived annotation.
 */
public abstract class AbstractDerivedAnnotation implements VcfAnnotation {

  private final String mName;
  private final String mDescription;
  private final AnnotationDataType mType;

  /**
   * @param name the attribute name
   * @param description the attribute description
   * @param type the attribute type
   */
  public AbstractDerivedAnnotation(String name, String description, AnnotationDataType type) {
    mName = name;
    mDescription = description;
    mType = type;
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public AnnotationDataType getType() {
    return mType;
  }

  /**
   * Get the VCF header description for the annotation.
   * @return the description of the annotation.
   */
  public String getDescription() {
    return mDescription;
  }

  protected String checkHeader(VcfHeader header, String[] infoFields, String[] formatFields) {
    final Set<String> infoHeaderIds = new HashSet<>();
    final Set<String> formatHeaderIds = new HashSet<>();
    if (header != null) {
      for (final InfoField field : header.getInfoLines()) {
        infoHeaderIds.add(field.getId());
      }
      for (final FormatField field : header.getFormatLines()) {
        formatHeaderIds.add(field.getId());
      }
    }
    final StringBuilder missingInfos = new StringBuilder();
    if (infoFields != null) {
      for (final String info : infoFields) {
        if (!infoHeaderIds.contains(info)) {
          missingInfos.append(' ').append(info);
        }
      }
    }
    final StringBuilder missingFormats = new StringBuilder();
    if (formatFields != null) {
      for (final String format : formatFields) {
        if (!formatHeaderIds.contains(format)) {
          missingFormats.append(' ').append(format);
        }
      }
    }
    final StringBuilder sb = new StringBuilder();
    if (missingInfos.length() > 0 || missingFormats.length() > 0) {
      sb.append("Derived annotation ").append(mName).append(" missing required fields in VCF header");
      if (missingInfos.length() > 0) {
        sb.append(" (INFO fields:").append(missingInfos.toString()).append(')');
      }
      if (missingFormats.length() > 0) {
        sb.append(" (FORMAT fields:").append(missingFormats.toString()).append(')');
      }
    }
    return sb.length() > 0 ? sb.toString() : null;
  }

}

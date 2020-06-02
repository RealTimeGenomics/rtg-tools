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
package com.rtg.util.diagnostic;

import java.io.Serializable;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

/**
 * Enumeration of SLIM information messages.
 * See <code>src.com.reeltwo.cartesian.util.diagnostic.Diagnostics.properties</code>
 * for the localised messages.
 *
 */
public final class InformationType implements DiagnosticType, PseudoEnum, Serializable {

  private static int sCounter = -1;

  /**
   * Simple User Message
   * You should supply one argument which is the message to display
   */
  public static final InformationType INFO_USER = new InformationType(++sCounter, "INFO_USER", 1);

  /**
   * Information about what file is being processed
   * <code>'Processing %1"%2" (%3 of %4)'</code>
   */
  public static final InformationType PROCESSING_ITEM_N_OF_N = new InformationType(++sCounter, "PROCESSING_ITEM_N_OF_N", 4);

  private static final EnumHelper<InformationType> HELPER = new EnumHelper<>(InformationType.class, new InformationType[] {
    INFO_USER,
    PROCESSING_ITEM_N_OF_N
  });

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str name of value
   * @return the enum value
   */
  public static InformationType valueOf(final String str) {
    return HELPER.valueOf(str);
  }

  /**
   * @return list of enum values
   */
  public static InformationType[] values() {
    return HELPER.values();
  }


  /** Number of parameters that must occur in conjunction with this information. */
  private final int mParams;

  private final int mOrdinal;

  private final String mName;

  private InformationType(final int ordinal, final String name, final int params) {
    mParams = params;
    mOrdinal = ordinal;
    mName = name;
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }

  @Override
  public String name() {
    return mName;
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public int getNumberOfParameters() {
    return mParams;
  }

  Object readResolve() {
    return values()[this.ordinal()];
  }

  @Override
  public String getMessagePrefix() {
    return "";
  }

}

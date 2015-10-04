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

package com.rtg.util.machine;

import java.util.Locale;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

/**
 * Enumeration of different machine types and associated information.
 */
public final class MachineType implements PseudoEnum {

  private static int sSequenceNumber = -1;

  /** The platform type for Illumina, regardless of whether it is paired or single end */
  public static final String PLAT_ILLUMINA = "ILLUMINA";

  /** The platform type for 454, regardless of whether it is paired or single end */
  public static final String PLAT_454 = "LS454";

  /** Illumina single end. */
  public static final MachineType ILLUMINA_SE = new MachineType(++sSequenceNumber, "illumina_se", "illumina", null, PLAT_ILLUMINA);

  /** Illumina paired end. */
  public static final MachineType ILLUMINA_PE = new MachineType(++sSequenceNumber, "illumina_pe", "illumina", MachineOrientation.FR, PLAT_ILLUMINA);

  /** Complete Genomics V1 (paired end). */
  public static final MachineType COMPLETE_GENOMICS = new MachineType(++sSequenceNumber, "complete_genomics", "complete", MachineOrientation.TANDEM, "COMPLETE");

  /** Complete Genomics V2 (paired end). */
  public static final MachineType COMPLETE_GENOMICS_2 = new MachineType(++sSequenceNumber, "complete_genomics_2", "complete_2", MachineOrientation.TANDEM, "COMPLETEGENOMICS", "\"Complete Genomics\"");

  /** Four Five Four paired end. */
  public static final MachineType FOURFIVEFOUR_PE = new MachineType(++sSequenceNumber, "454_pe", "ls454_pe", null, PLAT_454);

  /** Four Five Four single end. */
  public static final MachineType FOURFIVEFOUR_SE = new MachineType(++sSequenceNumber, "454_se", "ls454_se", null, PLAT_454);

  /** Ion Torrent (single end). */
  public static final MachineType IONTORRENT = new MachineType(++sSequenceNumber, "iontorrent", "iontorrent", null, "IONTORRENT");

  static final EnumHelper<MachineType> HELPER = new EnumHelper<>(MachineType.class, new MachineType[] {ILLUMINA_SE, ILLUMINA_PE, COMPLETE_GENOMICS, COMPLETE_GENOMICS_2, FOURFIVEFOUR_PE, FOURFIVEFOUR_SE, IONTORRENT});

  /**
   * @return list of the enum names
   */
  public static String[] names() {
    return HELPER.names();
  }

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str the name of the enum
   * @return the enum value
   */
  public static MachineType valueOf(final String str) {
    return HELPER.valueOf(str.toLowerCase(Locale.ROOT));
  }


  private final String mName;
  private final int mOrdinal;
  private final String mPriors;
  private final MachineOrientation mOrientation;
  private final String[] mPlatform;

  private MachineType(int ordinal, String name, String defaultPriors, MachineOrientation orientation, String... platform) {
    mName = name;
    mOrdinal = ordinal;
    mPriors = defaultPriors;
    mOrientation = orientation;
    mPlatform = platform;
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public String name() {
    return mName;
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }

  /**
   * Gets the name of the default priors for this machine
   * @return the default priors.
   */
  public String priors() {
    return mPriors;
  }

  /**
   * Get the <code>MachineOrientation</code>.
   * @return the <code>MachineOrientation</code> (may be null if unknown or single end data).
   */
  public MachineOrientation orientation() {
    return mOrientation;
  }

  /**
   * @return the string defining the platform of this machine type
   */
  public String platform() {
    return mPlatform[0];
  }

  /**
   * Compare if the platforms are compatible, e.g. <code>ILLUMINA</code> is equal to <code>illumina</code> or <code>Illumina</code>
   * @param platform other platform
   * @return if the two platforms are compatible
   */
  public boolean compatiblePlatform(final String platform) {
    for (String p : mPlatform) {
      if (p.equalsIgnoreCase(platform)) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@link EnumHelper#values()}
   * @return as in link
   */
  public static MachineType[] values() {
    return HELPER.values();
  }
}

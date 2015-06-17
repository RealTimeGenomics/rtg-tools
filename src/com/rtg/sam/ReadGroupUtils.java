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
package com.rtg.sam;

import com.rtg.util.machine.MachineType;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

/**
 */
public final class ReadGroupUtils {

  /** Read group attribute tag */
  public static final String RG_ATTRIBUTE = "RG";
  /** Name assigned to stats when no read group has been specified */
  public static final String UNKNOWN_RG = "unspecified";

  private ReadGroupUtils() { }

  /**
   * Get read group from <code>SAMRecord</code>
   * Will return 'unspecified' when read group is not set in record
   * @param record the SAM record to get the read group from
   * @return the read group
   */
  public static String getReadGroup(SAMRecord record) {
    final String rgAtt = record.getStringAttribute(RG_ATTRIBUTE);
    final String rgId;
    if (rgAtt == null) {
      rgId = UNKNOWN_RG;
    } else {
      rgId = "" + rgAtt;
    }
    return rgId;
  }

  /**
   * Get read group from <code>SamBamRecord</code>
   * Will return 'unspecified' when read group is not set in record
   * @param record the SAM record to get the read group from
   * @return the read group
   */
  public static String getReadGroup(SamBamRecord record) {
    final String rg = (String) record.getAttributeValue(RG_ATTRIBUTE);
    if (rg == null) {
      return UNKNOWN_RG;
    }
    return rg;
  }

  /**
   * Acquire machine type from read group if possible. Assumes platform has been set in read group
   * @param srgr the read group record.
   * @param paired if reads are paired or not
   * @return machine type if recognized, otherwise null
   */
  public static MachineType platformToMachineType(SAMReadGroupRecord srgr, boolean paired) {
    if (MachineType.COMPLETE_GENOMICS.compatiblePlatform(srgr.getPlatform())) {
      return MachineType.COMPLETE_GENOMICS;
    } else if (MachineType.FOURFIVEFOUR_PE.compatiblePlatform(srgr.getPlatform()) || MachineType.FOURFIVEFOUR_SE.compatiblePlatform(srgr.getPlatform())) {
      if (paired) {
        return MachineType.FOURFIVEFOUR_PE;
      } else {
        return MachineType.FOURFIVEFOUR_SE;
      }
    } else if (MachineType.ILLUMINA_PE.compatiblePlatform(srgr.getPlatform()) || MachineType.ILLUMINA_SE.compatiblePlatform(srgr.getPlatform())) {
      if (paired) {
        return MachineType.ILLUMINA_PE;
      } else {
        return MachineType.ILLUMINA_SE;
      }
    } else if (MachineType.IONTORRENT.compatiblePlatform(srgr.getPlatform())) {
      return MachineType.IONTORRENT;
    }
    return null;
  }
}

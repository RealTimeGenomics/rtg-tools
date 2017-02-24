/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.simulation.reads;

import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import com.rtg.util.cli.CFlags;
import com.rtg.util.machine.MachineType;

/**
 * Thin CLI layer for Complete genomics simulator
 */
public class CgSimCli extends ReadSimCli {

  static final String XMACHINE_ERROR_PRIORS = "Xmachine-errors";

  static final String CG_VERSION = "cg-read-version";

  protected static class CgSimValidator extends ReadSimCliValidator {
    @Override
    protected boolean checkMachines(CFlags cflags) {
      final int version = (Integer) cflags.getValue(CG_VERSION);
      if (version < 1 || version > 2) {
        cflags.setParseMessage("Version must be 1 or 2");
        return false;
      }
      return true;
    }
  }

  @Override
  public String moduleName() {
    return "cgsim";
  }

  @Override
  public String description() {
    return "generate simulated reads from a sequence";
  }

  @Override
  protected String getPriorsNameFlagValue() {
    if (mFlags.isSet(XMACHINE_ERROR_PRIORS)) {
      return (String) mFlags.getValue(XMACHINE_ERROR_PRIORS);
    }
    return null;
  }

  @Override
  protected MachineType getMachineType() {
    final int version = (Integer) mFlags.getValue(CG_VERSION);
    switch (version) {
      case 1:
        return MachineType.COMPLETE_GENOMICS;
      case 2:
        return MachineType.COMPLETE_GENOMICS_2;
      default:
        throw new UnsupportedOperationException("Unsupported CG version");
    }
  }

  @Override
  protected void initFlags() {
    super.initFlags();
    mFlags.setDescription("Simulate Complete Genomics Inc sequencing reads. Supports the original 35 bp read structure (5-10-10-10), and the newer 29 bp read structure (10-9-10)");
    mFlags.setCategories(UTILITY, new String[]{INPUT_OUTPUT, CAT_FRAGMENTS, CAT_CG, UTILITY});
    mFlags.setValidator(new CgSimValidator());
  }

  @Override
  protected void initMachineFlags() {
    mFlags.registerOptional('M', MAX_FRAGMENT, Integer.class, "int", "maximum fragment size", 500).setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional('m', MIN_FRAGMENT, Integer.class, "int", "minimum fragment size", 350).setCategory(CAT_FRAGMENTS);
    mFlags.registerOptional('E', XMACHINE_ERROR_PRIORS, String.class, "string", "override default machine error priors").setCategory(UTILITY);
    mFlags.registerRequired('V', CG_VERSION, Integer.class, "int", "select Complete Genomics read structure version, 1 (35 bp) or 2 (29 bp)").setCategory(CAT_CG);
  }
}

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

import java.util.Locale;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Validator;
import com.rtg.util.machine.MachineType;

/**
 * Flag validator for the read simulator.
 */
class ReadSimCliValidator implements Validator {

  @Override
  public boolean isValid(final CFlags cflags) {

    if (!cflags.checkInRange(ReadSimCli.MNP_EVENT_RATE, 0.0, 1.0)
      || !cflags.checkInRange(ReadSimCli.INS_EVENT_RATE, 0.0, 1.0)
      || !cflags.checkInRange(ReadSimCli.DEL_EVENT_RATE, 0.0, 1.0)
      || !cflags.checkXor(ReadSimCli.COVERAGE, ReadSimCli.READS)) {
      return false;
    }
    if (cflags.isSet(ReadSimCli.READS)) {
      if ((Long) cflags.getValue(ReadSimCli.READS) <= 0) {
        cflags.setParseMessage("Number of reads should be greater than 0");
        return false;
      }
    } else if (cflags.isSet(ReadSimCli.COVERAGE)) {
      final double coverage = (Double) cflags.getValue(ReadSimCli.COVERAGE);
      if (coverage <= 0.0) {
        cflags.setParseMessage("Coverage should be positive");
        return false;
      } else if (coverage > 1000000.0) {
        cflags.setParseMessage("Coverage cannot be greater than 1000000.0");
        return false;
      }
    }
    if (!CommonFlags.validateOutputDirectory(cflags)) {
      return false;
    }
    if (!cflags.checkNand(ReadSimCli.DISTRIBUTION, ReadSimCli.TAXONOMY_DISTRIBUTION)) {
      return false;
    }
    if (cflags.isSet(ReadSimCli.DISTRIBUTION) && !CommonFlags.validateInputFile(cflags, ReadSimCli.DISTRIBUTION)) {
        return false;
    }
    if (!cflags.isSet(ReadSimCli.TAXONOMY_DISTRIBUTION) && (cflags.isSet(ReadSimCli.ABUNDANCE) || cflags.isSet(ReadSimCli.DNA_FRACTION))) {
      cflags.setParseMessage("--" + ReadSimCli.ABUNDANCE + " and --" + ReadSimCli.DNA_FRACTION + " are only applicable if using --" + ReadSimCli.TAXONOMY_DISTRIBUTION);
      return false;
    }
    if (cflags.isSet(ReadSimCli.TAXONOMY_DISTRIBUTION) && !(cflags.isSet(ReadSimCli.ABUNDANCE) || cflags.isSet(ReadSimCli.DNA_FRACTION))) {
      cflags.setParseMessage("either --" + ReadSimCli.ABUNDANCE + " or --" + ReadSimCli.DNA_FRACTION + " must be set if using --" + ReadSimCli.TAXONOMY_DISTRIBUTION);
      return false;

    }
    if (!cflags.checkNand(ReadSimCli.ABUNDANCE, ReadSimCli.DNA_FRACTION)) {
      return false;
    }
    if (cflags.isSet(ReadSimCli.TAXONOMY_DISTRIBUTION) && !CommonFlags.validateInputFile(cflags, ReadSimCli.TAXONOMY_DISTRIBUTION)) {
      return false;
    }

    if (cflags.isSet(ReadSimCli.QUAL_RANGE)) {
      final String range = (String) cflags.getValue(ReadSimCli.QUAL_RANGE);
      final String[] vals = range.split("-");
      if (vals.length != 2) {
        cflags.setParseMessage("Quality range is not of form qualmin-qualmax");
        return false;
      } else {
        try {
          final int l = Integer.parseInt(vals[0]);
          if ((l < 0) || (l > 63)) {
            cflags.setParseMessage("Minimum quality value must be between 0 and 63");
            return false;
          }
          final int u = Integer.parseInt(vals[1]);
          if ((u < 0) || (u > 63)) {
            cflags.setParseMessage("Maximum quality value must be between 0 and 63");
            return false;
          }
          if (l > u) {
            cflags.setParseMessage("Minimum quality value cannot be greater than maximum quality value");
            return false;
          }
        } catch (final NumberFormatException e) {
          cflags.setParseMessage("Quality range is not of form qualmin-qualmax");
          return false;
        }
      }
    }
    if (!CommonFlags.validateSDF(cflags, ReadSimCli.INPUT)) {
      return false;
    }
    if ((Integer) cflags.getValue(ReadSimCli.MIN_FRAGMENT) > (Integer) cflags.getValue(ReadSimCli.MAX_FRAGMENT)) {
      cflags.setParseMessage("--" + ReadSimCli.MAX_FRAGMENT + " should not be smaller than --" + ReadSimCli.MIN_FRAGMENT);
      return false;
    }

    if (cflags.isSet(ReadSimCli.BED_FILE)) {
      if (!CommonFlags.validateInputFile(cflags, ReadSimCli.BED_FILE)) {
        return false;
      }
      if (cflags.isSet(ReadSimCli.COVERAGE)) {
        cflags.setParseMessage("--" + ReadSimCli.BED_FILE + " is incompatible with --" + ReadSimCli.COVERAGE);
        return false;
      }
    }

    return checkMachines(cflags);
  }

  protected boolean checkMachines(CFlags cflags) {
    final MachineType mt = MachineType.valueOf(cflags.getValue(ReadSimCli.MACHINE_TYPE).toString().toLowerCase(Locale.getDefault()));
    if (mt == MachineType.ILLUMINA_SE) {
      if (!cflags.checkRequired(ReadSimCli.READLENGTH)) {
        return false;
      }
      if ((Integer) cflags.getValue(ReadSimCli.READLENGTH) <= 1) {
        cflags.setParseMessage("Read length is too small");
        return false;
      }
      if ((Integer) cflags.getValue(ReadSimCli.READLENGTH) > (Integer) cflags.getValue(ReadSimCli.MIN_FRAGMENT)) {
        cflags.error("Read length is too large for selected fragment size");
        return false;
      }
      if (!cflags.checkBanned(ReadSimCli.LEFT_READLENGTH, ReadSimCli.RIGHT_READLENGTH, ReadSimCli.MIN_TOTAL_454_LENGTH, ReadSimCli.MAX_TOTAL_454_LENGTH, ReadSimCli.MIN_TOTAL_IONTORRENT_LENGTH, ReadSimCli.MAX_TOTAL_IONTORRENT_LENGTH)) {
        return false;
      }
    } else if (mt == MachineType.ILLUMINA_PE) {
      if (!cflags.checkRequired(ReadSimCli.LEFT_READLENGTH, ReadSimCli.RIGHT_READLENGTH)) {
        return false;
      }
      if (((Integer) cflags.getValue(ReadSimCli.LEFT_READLENGTH) <= 1)
        || ((Integer) cflags.getValue(ReadSimCli.RIGHT_READLENGTH) <= 1)) {
        cflags.error("Read length is too small");
        return false;
      }
      if (((Integer) cflags.getValue(ReadSimCli.LEFT_READLENGTH) > (Integer) cflags.getValue(ReadSimCli.MIN_FRAGMENT))
        || ((Integer) cflags.getValue(ReadSimCli.RIGHT_READLENGTH) > (Integer) cflags.getValue(ReadSimCli.MIN_FRAGMENT))) {
        cflags.error("Read length is too large for selected fragment size");
        return false;
      }
      if (!cflags.checkBanned(ReadSimCli.READLENGTH, ReadSimCli.MIN_TOTAL_454_LENGTH, ReadSimCli.MAX_TOTAL_454_LENGTH)) {
        return false;
      }

    } else if (mt == MachineType.COMPLETE_GENOMICS || mt == MachineType.COMPLETE_GENOMICS_2) {
      if (!cflags.checkBanned(ReadSimCli.READLENGTH, ReadSimCli.LEFT_READLENGTH, ReadSimCli.RIGHT_READLENGTH, ReadSimCli.MIN_TOTAL_454_LENGTH, ReadSimCli.MAX_TOTAL_454_LENGTH, ReadSimCli.MIN_TOTAL_IONTORRENT_LENGTH, ReadSimCli.MAX_TOTAL_IONTORRENT_LENGTH)) {
        return false;
      }

    } else if (mt == MachineType.FOURFIVEFOUR_PE || mt == MachineType.FOURFIVEFOUR_SE) {
      if (!cflags.checkRequired(ReadSimCli.MIN_TOTAL_454_LENGTH, ReadSimCli.MAX_TOTAL_454_LENGTH)) {
        return false;
      }
      if (!cflags.checkBanned(ReadSimCli.READLENGTH, ReadSimCli.LEFT_READLENGTH, ReadSimCli.RIGHT_READLENGTH, ReadSimCli.MIN_TOTAL_IONTORRENT_LENGTH, ReadSimCli.MAX_TOTAL_IONTORRENT_LENGTH)) {
        return false;
      }
      if ((Integer) cflags.getValue(ReadSimCli.MAX_TOTAL_454_LENGTH) > (Integer) cflags.getValue(ReadSimCli.MIN_FRAGMENT)) {
        cflags.error("Read length is too large for selected fragment size");
        return false;
      }
    } else if (mt == MachineType.IONTORRENT) {
      if (!cflags.checkRequired(ReadSimCli.MIN_TOTAL_IONTORRENT_LENGTH, ReadSimCli.MAX_TOTAL_IONTORRENT_LENGTH)) {
        return false;
      }
      if (!cflags.checkBanned(ReadSimCli.READLENGTH, ReadSimCli.LEFT_READLENGTH, ReadSimCli.RIGHT_READLENGTH, ReadSimCli.MIN_TOTAL_454_LENGTH, ReadSimCli.MAX_TOTAL_454_LENGTH)) {
        return false;
      }
      if ((Integer) cflags.getValue(ReadSimCli.MAX_TOTAL_IONTORRENT_LENGTH) > (Integer) cflags.getValue(ReadSimCli.MIN_FRAGMENT)) {
        cflags.error("Read length is too large for selected fragment size");
        return false;
      }
    } else {
      throw new IllegalArgumentException("Unhandled machine type: " + mt);
    }
    return true;
  }
}

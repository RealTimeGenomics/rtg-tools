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
package com.rtg.jmx;

import static com.rtg.jmx.MonUtils.NF0;
import static com.rtg.jmx.MonUtils.NF2;

import java.io.IOException;

/**
 * Output disk utilization stats.
 */
public class DiskStats extends ExternalCommand implements MonStats {

  private static final String IO_STAT = "/usr/bin/iostat";
  private static final String NA = "n/a";
  private static final String HEADERBOT = " r/s  w/s  rMB/s  wMB/s  %util";

  private final String mDisk;

  private final boolean mEnabled;

  DiskStats(String disk) {
    super(IO_STAT, "-xk", "1", "2", disk);
    mDisk = disk;
    boolean enabled = false;
    try {
      final String[] result = runCommand(IO_STAT, "-xk", "1", "1", disk);
      if (result != null) {
        for (String s : result) {
          if (s.contains(mDisk)) {
            enabled = true;
          }
        }
      }
    } catch (IOException e) {
      // Ignore
    }
    mEnabled = enabled;
  }

  @Override
  public void addHeader(Appendable out) {  }

  @Override
  public void addColumnLabelsTop(Appendable out) throws IOException {
    if (!mEnabled) {
      return;
    }
    MonUtils.padRight(out, "---Disk-" + mDisk, HEADERBOT.length(), '-');
  }

  @Override
  public void addColumnLabelsBottom(Appendable out) throws IOException {
    if (!mEnabled) {
      return;
    }
    out.append(HEADERBOT);
  }

  @Override
  public void addColumnData(Appendable out) throws IOException {
    if (!mEnabled) {
      return;
    }
    final String[] lines = runCommand();
    String rs = NA;
    String ws = NA;
    String rms = NA;
    String wms = NA;
    String util = NA;
    if (lines != null) {
      for (int i = lines.length - 1; i >= 0; --i) {
        //System.err.println("DISK line " + lines[i]);
        if (lines[i].contains(mDisk)) {
          final String[] res = lines[i].split("[  ]+");
          rs = NF0.format(Double.valueOf(res[3]));
          ws = NF0.format(Double.valueOf(res[4]));
          rms = NF2.format(Double.valueOf(res[5]) / 1024);
          wms = NF2.format(Double.valueOf(res[6]) / 1024);
          util = NF2.format(Double.valueOf(res[11]));
          break;
        }
      }
    }
    final int width = 6;
    MonUtils.pad(out, rs, 4);
    out.append(" ");
    MonUtils.pad(out, ws, 4);
    out.append(" ");
    MonUtils.pad(out, rms, width);
    out.append(" ");
    MonUtils.pad(out, wms, width);
    out.append(" ");
    MonUtils.pad(out, util, width);
  }
}

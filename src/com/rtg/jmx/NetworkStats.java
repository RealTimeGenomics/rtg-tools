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

import static com.rtg.jmx.MonUtils.NF2;

import java.io.IOException;

import com.rtg.util.Constants;

/**
 * Output network utilization stats.
 */
public class NetworkStats extends ExternalCommand implements MonStats {

  private static final String IFCONFIG = "/sbin/ifconfig";
  private static final String HEADERBOT = "txMB/s rxMB/s";
  private static final String NA = "n/a";
  private static final double MB = Constants.MB; // Convert to double


  private final boolean mEnabled;
  private final String mInterface;

  private long mTx = Long.MAX_VALUE;
  private long mRx = Long.MAX_VALUE;
  private long mTime = Long.MAX_VALUE;

  NetworkStats(String interfaceName) {
    super(IFCONFIG, interfaceName);
    boolean enabled = false;
    try {
      if (runCommand() != null) {
        enabled = true;
      }
    } catch (IOException e) {
      // Ignore
    }
    mEnabled = enabled;
    mInterface = interfaceName;
  }

  @Override
  public void addHeader(Appendable out) {  }

  @Override
  public void addColumnLabelsTop(Appendable out) throws IOException {
    if (!mEnabled) {
      return;
    }
    MonUtils.padRight(out, "--Net-" + mInterface, HEADERBOT.length(), '-');
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
    String txs = NA;
    String rxs = NA;
    if (lines != null) {
      for (int i = lines.length - 1; i >= 0; --i) {
        //System.err.println("network line : " + lines[i]);
        if (lines[i].contains("RX bytes")) {
          final String[] res = lines[i].trim().split("[  ]+");
          final long rx = Long.parseLong(res[1].split(":")[1]);
          final long tx = Long.parseLong(res[5].split(":")[1]);
          final long time = System.currentTimeMillis();
          final long dt = tx - mTx;
          final long dr = rx - mRx;
          final double dtime = (time - mTime) / 1000.0;
          mTx = tx;
          mRx = rx;
          mTime = time;
          if (dt >= 0 && dr >= 0) {
            txs = NF2.format(dt / dtime / MB);
            rxs = NF2.format(dr / dtime / MB);
          }
          break;
        }
      }
    }
    MonUtils.pad(out, txs, 6);
    out.append(" ");
    MonUtils.pad(out, rxs, 6);
  }

}

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.rtg.util.Constants;

/**
 * Output network utilization stats.
 */
public class NetworkStats implements MonStats {

  private static final String HEADERBOT = " txMB/s rxMB/s";
  private static final String NA = "n/a";
  private static final double MB = Constants.MB; // Convert to double

  private static final String RX_STATS_FILE_BASE = "/sys/class/net/%s/statistics/rx_bytes";
  private static final String TX_STATS_FILE_BASE = "/sys/class/net/%s/statistics/tx_bytes";

  private final boolean mEnabled;
  private final String mInterface;

  private long mTx = Long.MAX_VALUE;
  private long mRx = Long.MAX_VALUE;
  private long mTime = Long.MAX_VALUE;

  NetworkStats(String interfaceName) {
    mEnabled = getStatsFile(RX_STATS_FILE_BASE).exists() && getStatsFile(TX_STATS_FILE_BASE).exists();
    mInterface = interfaceName;
  }

  @Override
  public void addHeader(Appendable out) {  }

  @Override
  public void addColumnLabelsTop(Appendable out) throws IOException {
    if (!mEnabled) {
      return;
    }
    MonUtils.padRight(out, " --Net-" + mInterface, HEADERBOT.length(), '-');
  }

  @Override
  public void addColumnLabelsBottom(Appendable out) throws IOException {
    if (!mEnabled) {
      return;
    }
    out.append(HEADERBOT);
  }

  private File getStatsFile(String filePathBase) {
    return new File(String.format(filePathBase, mInterface));
  }

  private Long readFirstLineAsLong(String filePathBase) throws IOException {
    final File file = getStatsFile(filePathBase);
    if (file.exists()) {
      final Path path = file.toPath();
      final List<String> strings = Files.readAllLines(path);
      if (strings.size() > 0) {
        try {
          return Long.parseLong(strings.get(0));
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }
    return null;
  }

  private Long readRxBytes() throws IOException {
    return readFirstLineAsLong(RX_STATS_FILE_BASE);
  }

  private Long readTxBytes() throws IOException {
    return readFirstLineAsLong(TX_STATS_FILE_BASE);
  }

  @Override
  public void addColumnData(Appendable out) throws IOException {
    if (!mEnabled) {
      return;
    }
    String txs = NA;
    String rxs = NA;
    final Long tx = readTxBytes();
    final Long rx = readRxBytes();
    if (tx != null && rx != null) {
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
    }

    out.append(" ");
    MonUtils.pad(out, txs, 6);
    out.append(" ");
    MonUtils.pad(out, rxs, 6);
  }

}

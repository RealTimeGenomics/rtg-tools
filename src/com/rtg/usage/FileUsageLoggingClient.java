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
package com.rtg.usage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Implements a file based usage tracking client and server. Files written are based on the month and year
 */
public class FileUsageLoggingClient implements UsageLoggingClient {

  private static final int BUF_SIZE = 1024;
  private final File mUsageDir;
  private final UsageConfiguration mUsageConfiguration;
  private final boolean mRequireUsage;

  /**
   * @param usageDir directory in which to write usage tracking files
   * @param conf the usage tracking configuration (for user and host name logging options)
   * @param requireUsage true if usage tracking is mandatory, affects how error conditions are handled.
   */
  public FileUsageLoggingClient(File usageDir, UsageConfiguration conf, boolean requireUsage) {
    mUsageDir = usageDir;
    mUsageConfiguration = conf;
    mRequireUsage = requireUsage;
  }

  @Override
  public void recordBeginning(String module, UUID runId) {
    recordMessage(UsageMessage.startMessage(License.getSerialNumber(), runId.toString(), Environment.getVersion(), module));
  }

  @Override
  public void recordEnd(long metric, String module, UUID runId, boolean success) {
    recordMessage(UsageMessage.setMessage(License.getSerialNumber(), runId.toString(), Environment.getVersion(), module, Long.toString(metric), success ? "Success" : "Fail"));
  }

  private void recordMessage(UsageMessage message) {
    final Date d = new Date();
    message.setDate(d);
    if (mUsageConfiguration.logHostname()) {
      message.setHostname(Environment.getHostName());
    }
    if (mUsageConfiguration.logUsername()) {
      message.setUsername(System.getProperty("user.name"));
    }
    if (mUsageConfiguration.logCommandLine()) {
      message.setCommandLine(CommandLine.getCommandLine());
    }
    try {
      final File usageOut = ensureUsageFile(mUsageDir, d);
      try (final RandomAccessFile raf = new RandomAccessFile(usageOut, "rw");
           final FileLock lock = raf.getChannel().lock()) { //this blocks until lock is available, no timeout setting
        //get previous signage key thingy
        final String prevKey = getLastKey(raf);
        //write message
        raf.seek(raf.length());
        raf.write(message.formatLine(prevKey).getBytes());
        lock.release(); //this is because compiler complains about the lack of usage on this variable. Additionally it makes the intent clear about when this lock should be released
      }
    } catch (IOException e) {
      final String errorMessage = "Failed to record usage information (" + e.getMessage() + ")";
      if (mRequireUsage) {
        throw new NoTalkbackSlimException(errorMessage);
      } else {
        Diagnostic.warning(errorMessage);
      }
    }
  }

  static File ensureUsageFile(File usageDir, Date d) throws IOException {
    final File usageFile = getUsageFile(usageDir, d);
    if (!usageFile.exists()) {
      if (!usageDir.exists()) {
        if (!usageDir.mkdir()) {
          throw new IOException("Cannot create usage directory: " + usageDir.toString());
        } else {
          if (!usageDir.setReadable(true, false)
              || !usageDir.setWritable(true, false)) {
            throw new IOException("Failed to set permissions on usage directory:" + usageDir.toString());
          }
        }
      }
      // create file and set permissions so any user can write to it
      // file is created during random access and lock process
      try (final RandomAccessFile raf = new RandomAccessFile(usageFile, "rw");
          final FileLock lock = raf.getChannel().lock()) {

        if (!usageFile.setReadable(true, false)
            || !usageFile.setWritable(true, false)) {
          throw new IOException("Failed to set permissions on usage file:" + usageFile.toString());
        }
        lock.release();
      }
    }
    return usageFile;
  }

  static File getUsageFile(File usageDir, Date d) {
    final DateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM");
    return new File(usageDir, fileDateFormat.format(d) + ".usage");
  }

  static String getLastKey(RandomAccessFile raf) throws IOException {
    return getLastKey(raf, BUF_SIZE);
  }
  static String getLastKey(RandomAccessFile raf, int bufSize) throws IOException {
    final StringBuilder sb = new StringBuilder();
    final byte[] buf = new byte[bufSize];
    long endPos = raf.length();
    while (endPos != 0) {
      final long seekPos = Math.max(endPos - buf.length, 0);
      raf.seek(seekPos);
      final int length = (int) (endPos - seekPos);
      raf.readFully(buf, 0, length);
      sb.insert(0, new String(buf, 0, length));
      final String currentString = sb.toString();
      final int index = currentString.lastIndexOf(UsageMessage.SIGNATURE);
      if (index == -1) {
        final int chopIndex = Math.max(currentString.indexOf('\r'), currentString.indexOf('\n'));
        sb.delete(chopIndex + 1, sb.length());
      } else {
        final int lastSlashN = currentString.indexOf('\n', index);
        final int lastSlashR = currentString.indexOf('\r', index);
        //trying to find everything from the start of the signature up to the start of the newline (which can be \r\n, \n or \r)
        //not found == -1, so if only one was -1 take the larger (i.e. found) one, if both were found take the smaller (first found) one. If both are -1 it doesn't matter.
        final int lastNewline = lastSlashR != -1 && lastSlashN != -1 ? Math.min(lastSlashR, lastSlashN) : Math.max(lastSlashR, lastSlashN);
        final int endSig = lastNewline != -1 ? lastNewline : currentString.length();
        return currentString.substring(index + UsageMessage.SIGNATURE.length(), endSig);
      }
      endPos = seekPos;
    }
    return null;
  }

}

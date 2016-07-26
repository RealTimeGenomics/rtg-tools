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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import com.rtg.util.StringUtils;

/**
 * Captures stdout/stderr of tests
 */
class OutputListener extends RunListener {
  private static final String LINE = "-------------";
  private PrintStream mOriginalOut;
  private PrintStream mOriginalError;
  private LateOutputStream mOut = null;
  private LateOutputStream mErr = null;

  /**
   * An output stream that will hang around and redirect any output that occurs after it has been closed to a dedicated
   * output stream
   */
  private static class LateOutputStream extends OutputStream {
    private final ByteArrayOutputStream mOut;
    private final OutputStream mLate;
    private volatile boolean mClosed = false;
    private boolean mNotified = false;
    private final String mTestName;

    LateOutputStream(OutputStream late, String testName) {
      mTestName = testName;
      mOut = new ByteArrayOutputStream();
      mLate = late;

    }

    @Override
    public void write(int b) throws IOException {
      if (mClosed) {
        if (!mNotified) {
          mNotified = true;
          mLate.write(("Late output from " + mTestName + StringUtils.LS).getBytes());
        }
        mLate.write(b);
      } else {
        mOut.write(b);
      }
    }

    @Override
    public void close() throws IOException {
      mClosed = true;
    }

    public ByteArrayOutputStream getOut() {
      return mOut;
    }
  }

  @Override
  public void testStarted(Description description) throws Exception {
    mOriginalOut = System.out;
    mOriginalError = System.err;
    // It would be nice to close the print streams later, but we want to try and handle the case of
    // output being produced by background threads after the test finishes. We therefore don't know when to close
    // the print streams
    final String testName = RtgTestEntry.testName(description);
    mOut = new LateOutputStream(mOriginalOut, testName);
    final PrintStream outStream = new PrintStream(mOut);
    mErr = new LateOutputStream(mOriginalError, testName);
    final PrintStream errorStream = new PrintStream(mErr);
    System.setOut(outStream);
    System.setErr(errorStream);
  }

  @Override
  public void testFinished(Description description) throws Exception {
    final String name = RtgTestEntry.testName(description);
    if (mOut != null) {
      mOut.close();
      System.setOut(mOriginalOut);
      writeCollectedOutput(name, "STDOUT", mOut.getOut());
    }
    if (mErr != null) {
      mErr.close();
      System.setErr(mOriginalError);
      writeCollectedOutput(name, "STDERR", mErr.getOut());
    }
  }

  private static void writeCollectedOutput(String testName, String outputName, ByteArrayOutputStream out) {
    if (out.toString().length() > 0) {
      System.out.println();
      final String header = LINE + outputName + " for: " + testName + LINE;
      System.out.println(header);
      System.out.println(out.toString());
      System.out.println(StringUtils.repeat("-", header.length()));
    }
  }
}

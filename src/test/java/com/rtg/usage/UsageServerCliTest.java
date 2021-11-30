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
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.MD5Utils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class UsageServerCliTest extends TestCase {

  public void test() throws Exception {
    GlobalFlags.resetAccessedStatus();
    try (TestDirectory dir = new TestDirectory(); TestDirectory configDir = new TestDirectory();
         final MemoryPrintStream ps = new MemoryPrintStream()) {
      final File configFile = UsageConfiguration.createSimpleConfigurationFile(new File(configDir, "config"), dir.getPath(), null);
      final UsageServerCli cli = new UsageServerCli() {
        @Override
        protected int mainExec(OutputStream out, PrintStream err) throws IOException {
          mUsageLogger = new UsageLogging(new Properties(), moduleName(), CommandLine.getRunId(), configFile, true);
          return super.mainExec(out, err);
        }
      };

      final int[] code = new int[1];
      code[0] = 1001;
      final Runnable run = new Runnable() {
        @Override
        public void run() {
          final int returncode = cli.mainInit(new String[] {"-p", "3283"}, ps.outputStream(), ps.printStream());
          synchronized (cli.mSync) {
            code[0] = returncode;
          }
        }
      };
      final Thread serverThread = new Thread(run);
      serverThread.start();
      synchronized (cli.mSync) {
        while (!cli.getStarted() && code[0] == 1001) {
          cli.mSync.wait(50);
        }
      }

      UUID runId = UUID.randomUUID();
      if (cli.getStarted()) {
        final HttpUsageLoggingClient http = new HttpUsageLoggingClient("http://localhost:" + 3283 + "/usage", new UsageConfiguration(new Properties()), false);
        runId = UUID.randomUUID();
        http.recordBeginning("testModule", runId);
        http.recordEnd(65, "testModule", runId, true);
        serverThread.interrupt();
      }
      serverThread.join();
      assertEquals(ps.toString(), 0, code[0]);
      final File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(1, files.length);
      final File usageOut = files[0];
      final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
      assertEquals(df.format(new Date()) + ".usage", usageOut.getName());
      final String str = FileUtils.fileToString(usageOut);
      TestUtils.containsAll(str, License.getSerialNumber(), Environment.getVersion(), "testModule", "Start", "Success", runId.toString());
      final String[] outputLines = str.split("\r\n|\n");
      final String[] line1 = outputLines[0].split("\tS="); //splits into message and md5 sum
      assertEquals(MD5Utils.md5(line1[0]), line1[1]);
      final String[] line2 = outputLines[1].split("\tS=");
      assertEquals(MD5Utils.md5(line1[1] + line2[0]), line2[1]);
    }
  }
}

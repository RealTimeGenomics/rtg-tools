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

import java.text.SimpleDateFormat;
import java.util.UUID;

import com.rtg.util.MD5Utils;
import com.rtg.util.StringUtils;

import junit.framework.TestCase;

/**
 */
public class UsageMessageTest extends TestCase {

  private static final String FIRST_DATE = "2012-06-15 16:54:10";
  private static final String SECOND_DATE = "2012-06-15 16:54:11";

  public void test() throws Exception {
    final UUID uuid = new UUID(1234, 5678);
    final UsageMessage us = UsageMessage.startMessage("009", uuid.toString(), "rtg version test", "testclass");
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    us.setDate(sdf.parse(FIRST_DATE));
    final String firstLine = us.formatLine(null);
    final UsageMessage us2 = UsageMessage.setMessage("009", uuid.toString(), "rtg version test", "testclass", "999", "Success");
    us2.setDate(sdf.parse(SECOND_DATE));
    final String secondLine = us2.formatLine(us.getChecksum());
    final String expected1 = FIRST_DATE + "\t009\t00000000-0000-04d2-0000-00000000162e\trtg version test\ttestclass\tStart\tN/A\tN/A\tN/A\tN/A";
    final String expected1Sig = MD5Utils.md5(expected1);
    assertEquals(expected1 + "\tS=" + expected1Sig + StringUtils.LS, firstLine);
    final String expected2 = SECOND_DATE + "\t009\t00000000-0000-04d2-0000-00000000162e\trtg version test\ttestclass\tSuccess\t999\tN/A\tN/A\tN/A";
    final String expected2Sig = MD5Utils.md5(expected1Sig + expected2);
    assertEquals(expected2 + "\tS=" + expected2Sig + StringUtils.LS, secondLine);
  }

  public void test2() throws Exception {
    final UUID uuid = new UUID(1234, 5678);
    final UsageMessage us = UsageMessage.startMessage("009", uuid.toString(), "rtg version test", "testclass");
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    us.setDate(sdf.parse(FIRST_DATE));
    us.setUsername("bob");
    us.setHostname("jimbo");
    us.setCommandLine("FirstArg1" + StringUtils.LS + "SecondArg3");
    final String firstLine = us.formatLine(null);
    final UsageMessage us2 = UsageMessage.setMessage("009", uuid.toString(), "rtg version test", "testclass", "999", "Success");
    us2.setDate(sdf.parse(SECOND_DATE));
    us2.setUsername("bob");
    us2.setHostname("jimbo");
    us2.setCommandLine("FirstArg1" + StringUtils.LS + "SecondArg3");
    final String expectedCommandline = "FirstArg1" + StringUtils.getSpaceString(StringUtils.LS.length()) + "SecondArg3";
    final String secondLine = us2.formatLine(us.getChecksum());
    final String expected1 = FIRST_DATE + "\t009\t00000000-0000-04d2-0000-00000000162e\trtg version test\ttestclass\tStart\tN/A\tbob\tjimbo\t" + expectedCommandline;
    final String expected1Sig = MD5Utils.md5(expected1);
    assertEquals(expected1 + "\tS=" + expected1Sig + StringUtils.LS, firstLine);
    final String expected2 = SECOND_DATE + "\t009\t00000000-0000-04d2-0000-00000000162e\trtg version test\ttestclass\tSuccess\t999\tbob\tjimbo\t" + expectedCommandline;
    final String expected2Sig = MD5Utils.md5(expected1Sig + expected2);
    assertEquals(expected2 + "\tS=" + expected2Sig + StringUtils.LS, secondLine);
  }

  public void testTrims() {
    final UUID uuid = new UUID(1234, 5678);
    final UsageMessage us = UsageMessage.startMessage("009", uuid.toString(), "rtg version test", "testclass");
    final byte[] text = new byte[UsageMessage.HOSTNAME_TRIM_LENGTH + 5];
    us.setUsername(new String(text));
    us.setHostname(new String(text));

    assertEquals(UsageMessage.HOSTNAME_TRIM_LENGTH, us.mHostname.length());
    assertEquals(UsageMessage.USERNAME_TRIM_LENGTH, us.mUsername.length());
  }

  public void testCommandLineTrim() {
    final String onePart = "a quick brown fox\n jumps over the lazy dog\t repeat many many times";
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 50; i++) {
      sb.append(onePart);
    }
    assertTrue(sb.length() > 1000);
    final String cmdLine = UsageMessage.trimField(sb.toString(), UsageMessage.COMMANDLINE_TRIM_LENGTH);
    assertEquals(1000, cmdLine.length());
    assertTrue(cmdLine.contains("a quick brown fox  jumps over the lazy dog  repeat many many timesa quick brown fox  jumps"));
  }
}

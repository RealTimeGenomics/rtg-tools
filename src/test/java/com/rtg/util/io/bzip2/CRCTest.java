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
package com.rtg.util.io.bzip2;

import java.util.zip.CRC32;

/**
 * Test class
 */
public class CRCTest extends CBZip2InputStreamTest {

  public void testCRC() {
    //it seems that the bzip2 CRC algorithm uses a reversed bit order to that of the standard Crc32 used by gzip
    final CRC crc = new CRC();
    final CRC32 exp = new CRC32();
    crc.updateCRC(42);
    crc.updateCRC(42);
    crc.updateCRC(45);
    exp.update(84); //byte 42 reversed
    exp.update(84);
    exp.update(180); //byte 45 reversed

    assertEquals(Long.toBinaryString(exp.getValue()), new StringBuilder(Long.toBinaryString(crc.getFinalCRC() & 0xffffffffL)).reverse().toString());

    final CRC crc2 = new CRC();
    crc2.updateCRC(42, 2);
    crc2.updateCRC(45, 1);

    assertEquals(Long.toBinaryString(exp.getValue()), new StringBuilder(Long.toBinaryString(crc2.getFinalCRC() & 0xffffffffL)).reverse().toString());
  }

}

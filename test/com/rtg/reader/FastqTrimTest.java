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

package com.rtg.reader;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.mode.DnaUtils;
import com.rtg.util.NullStreamUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.TestDirectory;

/**
 */
public class FastqTrimTest  extends AbstractCliTest {

  @Override
  public void setUp() throws IOException {
    super.setUp();
    Diagnostic.setLogStream(NullStreamUtils.getNullPrintStream());
  }
  @Override
  protected AbstractCli getCli() {
    return new FastqTrim();
  }


  public void testHelp() {
    checkHelp("rtg fastqtrim"
      , "-i", "--input=FILE", "input FASTQ file"
      , "-o", "--output=FILE", "output filename. Use '-' to write to standard output"
      , "-q", "--quality-format=FORMAT", "quality data encoding", "Allowed values are [sanger, solexa, illumina] (Default is sanger)"
      , "-e", "--trim-end-bases=INT", "always trim the specified number of bases from read end (Default is 0)"
      , "-s", "--trim-start-bases=INT", "always trim the specified number of bases from read start (Default is 0)"
      , "--end-quality-threshold=INT", "trim read ends to maximise base quality above the given threshold (Default is 0)"
      , "-Z", "--no-gzip", "do not gzip the output"
      , "-T", "--threads=INT", "number of threads (Default is the number of available cores)"
    );
    checkExtendedHelp("rtg fastqtrim",
      "--Xbatch-size=INT",  "number of reads to process per batch (Default is 100000)"
    );
  }

  CFlags getFlags(boolean valid, String... strings) {

    final CFlags flags = new CFlags("fastqtrim", "", NullStreamUtils.getNullPrintStream(), NullStreamUtils.getNullPrintStream());
    FastqTrim.initFlags(flags);
    Assert.assertEquals("Flags were unexpectedly " + (valid ? "invalid" : "valid"), valid, flags.setFlags(strings));
    return flags;
  }

  public void testFlags() throws IOException {
    assertParseMessage("You must provide a value for -o FILE", "-i", "foo");
    try (TestDirectory tmp = new TestDirectory()) {
      final File[] files = {
        new File(tmp, "foo"),
        new File(tmp, "bar.fastq.gz"),
      };
      for (File f : files) {
        assertTrue(f.createNewFile());
      }
      getFlags(false, "-i", files[0].getPath(), "-o", files[1].getPath());
      assertTrue(files[1].delete());
      getFlags(true, "-i", files[0].getPath(), "-o", files[1].getPath());
      //assertParseMessage("The value for --", "-l", files[0].getPath(), "-r", files[1].getPath(), "-o", files[2].getPath(), "-S", "-1");
    }
  }

  private void assertParseMessage(String expected, String... strings) {
    final CFlags flags = getFlags(false, strings);
    assertEquals(expected, flags.getParseMessage());
  }

  public void testNullTrimmer() {
    final ReadTrimmer trimmer = FastqTrim.getTrimmer(0, 0, 0, 0, 0);
    final String readString = "ACGTGGGAGATTTGATG";
    final FastqSequence read = FastqSequenceTest.getFastq("sequence", readString);
    checkTrimmer(read, readString, trimmer);
  }

  public void testStartTrimmer() {
    final ReadTrimmer trimmer = FastqTrim.getTrimmer(1, 0, 0, 0, 0);
    final String readString = "ACGTGGGAGATTTGATG";
    final FastqSequence read = FastqSequenceTest.getFastq("sequence", readString);
    checkTrimmer(read, readString.substring(1), trimmer);
  }

  public void testEndTrimmer() {
    final ReadTrimmer trimmer = FastqTrim.getTrimmer(0, 0, 1, 0, 0);
    final String readString = "ACGTGGGAGATTTGATG";
    final FastqSequence read = FastqSequenceTest.getFastq("sequence", readString);
    checkTrimmer(read, readString.substring(0, readString.length() - 1), trimmer);
  }

  public void testStartQualityTrimmer() {
    final ReadTrimmer trimmer = FastqTrim.getTrimmer(0, 27, 0, 0, 0);
    final String readString = "ACGTGGGAGATTTGATG";
    final FastqSequence read = FastqSequenceTest.getFastq("sequence", readString, FastaUtils.asciiToRawQuality(";;;;;;;;EEEEEEEEE"));
    checkTrimmer(read, "GATTTGATG", trimmer);
  }

  public void testEndQualityTrimmer() {
    final ReadTrimmer trimmer = FastqTrim.getTrimmer(0, 0, 0, 27, 0);
    final String readString = "ACGTGGGAGATTTGATG";
    final FastqSequence read = FastqSequenceTest.getFastq("sequence", readString, FastaUtils.asciiToRawQuality("EEEEEEEEE;;;;;;;;"));
    checkTrimmer(read, "ACGTGGGAG", trimmer);
  }

  public void testMinLengthTrimmer() {
    final ReadTrimmer trimmer = FastqTrim.getTrimmer(0, 0, 0, 0, 18);
    final String readString = "ACGTGGGAGATTTGATG";
    final FastqSequence read = FastqSequenceTest.getFastq("sequence", readString);
    checkTrimmer(read, "", trimmer);
  }

  public void testMinLengthAfterTrim() {
    final ReadTrimmer trimmer = FastqTrim.getTrimmer(4, 0, 3, 0, 11);
    final String readString = "ACGTGGGAGATTTGATG";
    final FastqSequence read = FastqSequenceTest.getFastq("sequence", readString);
    checkTrimmer(read, "", trimmer);

  }


  public void checkTrimmer(FastqSequence seq, String expected, ReadTrimmer trimmer) {
    seq.trim(trimmer);
    assertEquals(expected, DnaUtils.bytesToSequenceIncCG(seq.getBases()));
  }

}
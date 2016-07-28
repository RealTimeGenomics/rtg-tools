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
package com.rtg.reference;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.rtg.reader.ReaderUtils;
import com.rtg.util.io.FileUtils;

/**
 * Generates a generic reference.txt
 */
public final class ReferenceTextBuilder {

  private final StringBuilder mText;

  private ReferenceTextBuilder(String initial) {
    mText = new StringBuilder(initial);
  }

  /**
   * Get a builder with the default set to haploid sequences
   * @return the builder
   */
  public static ReferenceTextBuilder createHaploid() {
    return new ReferenceTextBuilder(ReferenceGenome.REFERENCE_DEFAULT_HAPLOID);
  }

  /**
   * Get a builder with the default set to diploid sequences
   * @return the builder
   */
  public static ReferenceTextBuilder createDiploid() {
    return new ReferenceTextBuilder(ReferenceGenome.REFERENCE_DEFAULT_DIPLOID);
  }

  /**
   * Add a sequence specification
   * @param name name of the sequence
   * @param sex which sex the specification applies to
   * @param ploidy which ploidy the specification applies to
   * @param linear whether the sequence is linear or circular
   * @return this instance with the sequence added
   */
  public ReferenceTextBuilder addSequence(String name, Sex sex, Ploidy ploidy, boolean linear) {
    mText.append(sex.name().toLowerCase(Locale.getDefault()));
    mText.append("\t");
    mText.append("seq");
    mText.append("\t");
    mText.append(name);
    mText.append("\t");
    mText.append(ploidy.name().toLowerCase(Locale.getDefault()));
    mText.append("\t");
    mText.append(linear ? "linear" : "circular");
    mText.append("\n");
    return this;
  }

  /**
   * write the reference.txt to a file
   * @param filename the file to write to
   * @throws IOException if an IO error occurs
   */
  public void writeToFile(File filename) throws IOException {
    FileUtils.stringToFile(mText.toString(), filename);
  }

  /**
   * install this reference.txt into an SDF
   * @param sdfDir the SDF directory
   * @throws IOException if an IO error occurs, or supplied directory does not contain an SDF
   */
  public void writeToSdfDir(File sdfDir) throws IOException {
    if (ReaderUtils.isSDF(sdfDir)) {
      writeToFile(new File(sdfDir, ReferenceGenome.REFERENCE_FILE));
    } else {
      throw new IOException(String.format("%s is not an SDF", sdfDir.getPath()));
    }
  }

  /**
   * @return string representation of this reference.txt
   */
  public String toString() {
    return mText.toString();
  }
}

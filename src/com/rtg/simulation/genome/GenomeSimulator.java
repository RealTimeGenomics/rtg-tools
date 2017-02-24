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

package com.rtg.simulation.genome;

import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.util.PortableRandom;
import com.rtg.util.array.ArrayUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.io.LogStream;

/**
 * Module wrapper for the standard genome type generators.
 *
 */
public class GenomeSimulator extends LoggedCli {

  static final String MODULE_NAME = "genomesim";

  static final String DEFAULT_PREFIX = "simulatedSequence";

  static final String SEED = "seed";
  //static final String NO_SEED = "no-seed";
  //static final String GENERATE_GENOME = "genome-size";
  static final String FREQUENCY = "freq";
  static final String NUM_CONTIGS = "num-contigs";
  static final String MIN_LENGTH = "min-length";
  static final String MAX_LENGTH = "max-length";
  static final String PREFIX = "prefix";
  static final String LENGTH = "length";
  static final String COMMENT = "comment";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "generate simulated genome sequence";
  }

  @Override
  protected void initFlags() {
    //flags.registerExtendedHelp();
    mFlags.setDescription("Simulates a reference genome.");
    CommonFlagCategories.setCategories(mFlags);

    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, "SDF", "output SDF").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional('s', SEED, Integer.class, "int", "seed for random number generator").setCategory(UTILITY);
    final Flag numContigs = mFlags.registerOptional('n', NUM_CONTIGS, Integer.class, "int", "number of sequences to generate").setCategory(UTILITY);
    final Flag maxLength = mFlags.registerOptional(MAX_LENGTH, Integer.class, "int", "maximum sequence length").setCategory(UTILITY);
    final Flag minLength = mFlags.registerOptional(MIN_LENGTH, Integer.class, "int", "minimum sequence length").setCategory(UTILITY);
    final Flag lFlag = mFlags.registerOptional('l', LENGTH, Integer.class, "int", "length of generated sequence");
    lFlag.setMaxCount(Integer.MAX_VALUE).enableCsv();
    lFlag.setCategory(UTILITY);
    mFlags.registerOptional(FREQUENCY, String.class, "string", "relative frequencies of A,C,G,T in the generated sequence", "1,1,1,1").setCategory(UTILITY);
    mFlags.registerOptional(COMMENT, String.class, "string", "comment to include in the generated SDF").setCategory(UTILITY);
    mFlags.registerOptional(PREFIX, String.class, "string", "sequence name prefix", DEFAULT_PREFIX).setCategory(UTILITY);

    mFlags.addRequiredSet(numContigs, minLength, maxLength);
    mFlags.addRequiredSet(lFlag);
    mFlags.setValidator(new GenomeSimulatorFlagValidator());
  }

  private static class GenomeSimulatorFlagValidator implements Validator {

    @Override
    public boolean isValid(final CFlags cflags) {
      if (!cflags.isSet(LENGTH)) {
        // error: Lengths 0, not both min and max set
        if (!(cflags.isSet(MAX_LENGTH) && cflags.isSet(MIN_LENGTH) && cflags.isSet(NUM_CONTIGS))) {
          cflags.setParseMessage("Must either specify explicit sequence length OR number of sequences plus minimum and maximum lengths.");
          return false;
        } else {
          // ok: Lengths 0, and both min and max set
          if (!cflags.checkMinMaxInRange(MIN_LENGTH, MAX_LENGTH, 0, false, Integer.MAX_VALUE, true)) {
            return false;
          }
          if (!cflags.checkInRange(NUM_CONTIGS, 0, Integer.MAX_VALUE)) {
            return false;
          }
        }
      } else {
        // error: Lengths given, but also either or both min and max set
        if (cflags.isSet(MAX_LENGTH) || cflags.isSet(MIN_LENGTH) || cflags.isSet(NUM_CONTIGS)) {
          cflags.setParseMessage("Must either specify explicit sequence length OR number of sequences plus minimum and maximum lengths.");
          return false;
        }
        final Collection<Object> lengths = cflags.getValues(LENGTH);
        for (Object obj : lengths) {
          final int length = (Integer) obj;
          if (length <= 0) {
            cflags.setParseMessage("Length values must be greater than 0");
            return false;
          }
        }
        // ok: Lengths given, not both min and max set
      }

      if (!CommonFlags.validateOutputDirectory(cflags)) {
        return false;
      }

      if (cflags.isSet(FREQUENCY)) {
        try {
          final int[] freqDist = ArrayUtils.parseIntArray((String) cflags.getValue(FREQUENCY));
          if (freqDist.length != 4) {
            cflags.setParseMessage("Expected four comma separated integers for frequency distribution.");
            return false;
          }
          int sum = 0;
          for (final int freq : freqDist) {
            if (freq < 0) {
              cflags.setParseMessage("Expected non-negative integers for frequency distribution.");
              return false;
            }
            sum += freq;
          }
          if (sum <= 0) {
            cflags.setParseMessage("Expected at least one non-zero integer for frequency distribution.");
            return false;
          }
        } catch (final NumberFormatException e) {
          cflags.setParseMessage("Invalid frequency distribution format.");
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Invoke the genome generator
   *
   * @return 0 on success 1 on failure
   * @throws IOException if an I/O error occurs
   */
  public int generateGenome() throws IOException {
    final SequenceGenerator generator;
    final int[] freqDist;
    final int[] lengths;
    final int numSequences;
    final PortableRandom rand;

    if (!mFlags.isSet(SEED)) {
      rand = new PortableRandom();
    } else {
      rand = new PortableRandom((Integer) mFlags.getValue(SEED));
    }

    if (mFlags.isSet(LENGTH)) {
      numSequences = mFlags.getValues(LENGTH).size();
      lengths = new int[numSequences];
      int i = 0;
      for (final Object item : mFlags.getValues(LENGTH)) {
        lengths[i++] = (Integer) item;
      }
    } else {
      numSequences = (Integer) mFlags.getValue(NUM_CONTIGS);
      lengths = new int[numSequences];
      final int diff = (Integer) mFlags.getValue(MAX_LENGTH) - (Integer) mFlags.getValue(MIN_LENGTH);
      if (diff > 0) {
        for (int i = 0; i < numSequences; ++i) {
          lengths[i] = rand.nextInt(diff + 1) + (Integer) mFlags.getValue(MIN_LENGTH);
        }
      } else {
        for (int i = 0; i < numSequences; ++i) {
          lengths[i] = (Integer) mFlags.getValue(MIN_LENGTH);
        }

      }
    }

    freqDist = ArrayUtils.parseIntArray((String) mFlags.getValue(FREQUENCY));

    final RandomDistribution frequencyDistribution = new RandomDistribution(freqDist, rand);

    generator = new SequenceGenerator(rand, frequencyDistribution, lengths, (File) mFlags.getValue(OUTPUT_FLAG), (String) mFlags.getValue(PREFIX));
    generator.setComment((String) mFlags.getValue(COMMENT));
    generator.createSequences();
    return 0;
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    return generateGenome();
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(OUTPUT_FLAG);
  }

}

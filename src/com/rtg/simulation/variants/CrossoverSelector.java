/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.simulation.variants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.rtg.reference.ReferenceSequence;
import com.rtg.reference.Sex;
import com.rtg.util.PortableRandom;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Models crossover position selection, allowing either uniform position selection, or
 * selection according to explicit genetic map.
 */
public class CrossoverSelector {

  private final boolean mInterpolate;
  private final File mGeneticMapDir;
  private final double mExtraCrossoverFreq;
  private final Map<String, GeneticMap> mGeneticMaps = new HashMap<>();

  interface GeneticMap {
    int choosePosition(PortableRandom random);
  }


  /**
   * Constructor
   * @param geneticMapDir directory containing genetic maps, or null to alway use uniform distribution.
   * @param extraCrossovers expected number of extra crossovers per chromosome
   * @param interpolate if true, interpolate genetic maps
   */
  public CrossoverSelector(File geneticMapDir, double extraCrossovers, boolean interpolate) {
    mGeneticMapDir = geneticMapDir;
    mExtraCrossoverFreq = extraCrossovers;
    mInterpolate = interpolate;
  }


  /**
   * Selects crossover positions according to probabilities specified in an explicit genetic map.
   */
  class FileGeneticMap implements GeneticMap {

    static final String EXT = ".map";
    private static final int INITIAL_SIZE = 10;
    private final double[] mCdf;
    private final int[] mPos;
    private final File mMapFile;

    FileGeneticMap(final File mapFile) throws IOException {
      if (!mapFile.exists()) {
        throw new IOException("Expected genetic map file " + mapFile + " does not exist");
      }
      mMapFile = mapFile;
      Diagnostic.userLog("Loading genetic map from " + mapFile);
      try (BufferedReader br = new BufferedReader(new FileReader(mapFile))) {
        double[] centimorganList = new double[INITIAL_SIZE];
        int[] posList = new int[INITIAL_SIZE];
        int size = 0;

        // First line is header, try and find expected columns
        String line = br.readLine();
        int posCol = -1;
        int centimorganCol = -1;
        if (line != null) {
          final String[] header = line.split("\t");
          for (int k = 0; k < header.length; ++k) {
            if ("pos".equals(header[k])) {
              posCol = k;
            } else if ("cM".equals(header[k])) {
              centimorganCol = k;
            }
          }
        }
        if (posCol == -1 || centimorganCol == -1) {
          throw new IOException("Expected first line of genetic map to contain a header line including \"pos\" and \"cM\" columns");
        }

        // Record the "pos" and "cM" information
        while ((line = br.readLine()) != null) {
          final String[] words = line.split("\t");
          if (size == centimorganList.length) {
            centimorganList = Arrays.copyOf(centimorganList, centimorganList.length * 2);
            posList = Arrays.copyOf(posList, posList.length * 2);
          }
          posList[size] = Integer.parseInt(words[posCol]);
          centimorganList[size] = Double.parseDouble(words[centimorganCol]);
          size++;
        }
        // Convert delta-cM to cumulative probability
        mCdf = new double[centimorganList.length];
        double p = 0;
        for (int k = 0; k < mCdf.length; ++k) {
          p += centimorgansToProb(centimorganList[k] - (k == 0 ? 0 : centimorganList[k - 1]));
          mCdf[k] = p;
        }
        // Normalize
        final double max = mCdf[mCdf.length - 1];
        if (max <= 0) {
          throw new NoTalkbackSlimException("Genetic map file does not contain non-zero distribution");
        }
        for (int k = 0; k < mCdf.length; ++k) {
          mCdf[k] /= max;
        }
        mPos = Arrays.copyOf(posList, size);
      }
    }

    // Find the position in the CDF is that random number
    int findPos(double prob) {
      for (int j = 0; j < mCdf.length - 1 ; j++) {
        if (prob >= mCdf[j] && prob < mCdf[j + 1]) {
          if (mInterpolate) {
            final double frac = prob - mCdf[j] / (mCdf[j + 1] - mCdf[j]);
            final int delta = (int) (frac * (mPos[j + 1] - mPos[j]));
            return mPos[j] + delta;
          } else {
            return mPos[j];
          }
        }
      }
      return 0;
    }

    @Override
    public String toString() {
      return "Map:" + mMapFile.getName();
    }

    @Override
    public int choosePosition(PortableRandom random) {
      return findPos(random.nextDouble());
    }

    private double centimorgansToProb(final double cM) {
      return cM <= 0 ? 0 : 0.5 * (1 - Math.exp(-cM / 50));
    }
  }

  GeneticMap getGeneticMap(ReferenceSequence refSeq, Sex sex) throws IOException {
    final String mapName = mapName(refSeq, sex);
    GeneticMap map = mGeneticMaps.get(mapName);
    if (map == null) {
      if (mGeneticMapDir != null) {
        final File mapFile = new File(mGeneticMapDir, mapName);
        if (mapFile.exists()) {
          map = new FileGeneticMap(mapFile);
        } else {
          // Sex specific map file did not exist, check for a non-specific version
          final File nonspecificMapFile = new File(mGeneticMapDir, mapName(refSeq, null));
          if (nonspecificMapFile.exists()) {
            map = new FileGeneticMap(nonspecificMapFile);
          } else {
            Diagnostic.warning("Genetic map file " + mapFile + " does not exist, using uniform distribution");
          }
        }
      }
      if (map == null) {
        // Fall back to uniform distribution
        map = new GeneticMap() {
          final int mSeqLength = refSeq.length();
          @Override
          public int choosePosition(PortableRandom random) {
            return random.nextInt(mSeqLength);
          }
          @Override
          public String toString() {
            return "Uniform:" + mSeqLength;
          }
        };
      }
      mGeneticMaps.put(mapName, map);
    }
    return map;
  }

  static String mapName(ReferenceSequence refSeq, Sex sex) {
    return sex == null
      ? refSeq.name() + FileGeneticMap.EXT
      : sex.name().toLowerCase(Locale.getDefault()) + "." + refSeq.name() + FileGeneticMap.EXT;
  }

  /**
   * Get the recombination points for a specified chromosome. There will be at least one, and there may be more.
   * @param random supplier of randomness
   * @param refSeq the reference sequence to select crossover locations for
   * @param sex the sex
   * @return the selected recombination positions
   * @throws IOException if there is a problem reading a genetic map.
   */
  public int[] getCrossoverPositions(PortableRandom random, ReferenceSequence refSeq, Sex sex) throws IOException {
    final int[] crossoverPoints = new int[1 + (random.nextDouble() < mExtraCrossoverFreq ? 1 : 0)];
    final GeneticMap geneticMap = getGeneticMap(refSeq, sex);
    for (int i = 0; i < crossoverPoints.length; i++) {
      crossoverPoints[i] = geneticMap.choosePosition(random);
    }
    Arrays.sort(crossoverPoints);
    return crossoverPoints;
  }
}

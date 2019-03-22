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

    static final String EXT = ".CDF.txt";
    static final String GENETIC_MAP_HEADER = "chr\tpos\tprob\tcdf";

    private static final int INITIAL_SIZE = 10;
    private static final int POS_COL = 1;
    private static final int CDF_COL = 3;
    private final double[] mCdf;
    private final int[] mPos;
    private final File mMapFile;

    FileGeneticMap(File mapFile) throws IOException {
      if (!mapFile.exists()) {
        throw new IOException("Expected genetic map file " + mapFile + " does not exist");
      }
      mMapFile = mapFile;
      Diagnostic.userLog("Loading genetic map from " + mapFile);
      try (BufferedReader br = new BufferedReader(new FileReader(mapFile))) {
        double[] cdfList = new double[INITIAL_SIZE];
        int[] posList = new int[INITIAL_SIZE];
        int size = 0;
        String line = br.readLine(); // First line is header
        if (line == null || !line.startsWith(GENETIC_MAP_HEADER)) {
          throw new IOException("Expected first line of genetic map to contain: " + GENETIC_MAP_HEADER);
        }
        while ((line = br.readLine()) != null) {
          final String[] words = line.split("\t");
          if (size == cdfList.length) {
            cdfList = Arrays.copyOf(cdfList, cdfList.length * 2);
            posList = Arrays.copyOf(posList, posList.length * 2);
          }
          posList[size] = Integer.parseInt(words[POS_COL]);
          cdfList[size] = Double.parseDouble(words[CDF_COL]);
          size++;
        }
        mCdf = Arrays.copyOf(cdfList, size);
        mPos = Arrays.copyOf(posList, size);
      }
    }
    //find the position in the CDF is that random number
    int findPos(double prob) {
//      int i = SimulationUtils.chooseFromCumulative(mCdf, prob);
      for (int j = 0; j < mCdf.length - 1 ; j++) {
        if (prob >= mCdf[j] && prob < mCdf[j + 1]) {
//          if (j != i) {
//            System.err.println("Different selection position than chooseFromCumulative: " + prob + " " + i + " " + j);
//          }
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
          Diagnostic.warning("Genetic map file " + mapFile + " does not exist, using uniform distribution");
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
    return sex.name().toLowerCase(Locale.getDefault()) + "." + refSeq.name() + FileGeneticMap.EXT;
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
    final GeneticMap gmap = getGeneticMap(refSeq, sex);
    for (int i = 0; i < crossoverPoints.length; i++) {
      crossoverPoints[i] = gmap.choosePosition(random);
    }
    Arrays.sort(crossoverPoints);
    return crossoverPoints;
  }
}

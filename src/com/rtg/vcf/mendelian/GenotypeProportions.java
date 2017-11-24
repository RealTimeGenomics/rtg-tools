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
package com.rtg.vcf.mendelian;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.rtg.util.MultiSet;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * Accumulates proportions of child GT calls for each mother/father GT combination
 */
public final class GenotypeProportions {

  /**
   * Constructor
   */
  public GenotypeProportions() { }

  private final TreeMap<Parents, MultiSet<Genotype>> mAlleleSets = new TreeMap<>(Parents.PARENTS_COMPARATOR);


  /**
   * Increment the counts associated with this combination of genotypes
   * @param parent1 genotype for first parent
   * @param parent2 genotype for second parent
   * @param child genotype for child
   */
  public void addRecord(Genotype parent1, Genotype parent2, Genotype child) {
    final Parents parents = new Parents(parent1, parent2);
    addRecord(parents, child);
  }
  void addRecord(Parents parents, Genotype child) {
    addRecord(parents, child, 1);
  }
  void addRecord(Parents parents, Genotype child, int count) {
    if (!mAlleleSets.containsKey(parents)) {
      mAlleleSets.put(parents, new MultiSet<>(new TreeMap<>(Genotype.GENOTYPE_COMPARATOR)));
    }
    final MultiSet<Genotype> childAlleles = mAlleleSets.get(parents);
    childAlleles.add(child, count);
  }


  /**
   * @return a new set of genotype proportions corresponding only to non-multiallelic genotypes
   */
  public GenotypeProportions filterMultiallelic() {
    final GenotypeProportions result = new GenotypeProportions();
    for (Map.Entry<Parents, MultiSet<Genotype>> entry : mAlleleSets.entrySet()) {
      final Parents parents = entry.getKey();
      if (!parents.mParents[0].multiallelic() && !parents.mParents[1].multiallelic()) {
        final MultiSet<Genotype> childAlleles = mAlleleSets.get(parents);
        for (Genotype child : childAlleles.keySet()) {
          if (!child.multiallelic()) {
            result.addRecord(parents, child, childAlleles.get(child));
          }
        }
      }
    }
    return result;
  }

  /**
   * @return a new set of genotype proportions corresponding only to cases where all genotypes are diploid
   */
  public GenotypeProportions filterNonDiploid() {
    final GenotypeProportions result = new GenotypeProportions();
    for (Map.Entry<Parents, MultiSet<Genotype>> entry : mAlleleSets.entrySet()) {
      final Parents parents = entry.getKey();
      if (parents.mParents[0].length() == 2 && parents.mParents[1].length() == 2) {
        final MultiSet<Genotype> childAlleles = mAlleleSets.get(parents);
        for (Genotype child : childAlleles.keySet()) {
          if (child.length() == 2) {
            result.addRecord(parents, child, childAlleles.get(child));
          }
        }
      }
    }
    return result;
  }

  /**
   * @return a new set of genotype proportions where parent 1 and parent 2 represents a canonical ordering rather than father and mother
   */
  public GenotypeProportions canonicalParents() {
    final GenotypeProportions result = new GenotypeProportions();
    for (Map.Entry<Parents, MultiSet<Genotype>> entry : mAlleleSets.entrySet()) {
      final Parents parents = entry.getKey();
      final Parents canonical = Genotype.GENOTYPE_COMPARATOR.compare(parents.mParents[0], parents.mParents[1]) > 0
        ? parents : new Parents(parents.mParents[1], parents.mParents[0]);
      final MultiSet<Genotype> childAlleles = mAlleleSets.get(parents);
      for (Genotype child : childAlleles.keySet()) {
        result.addRecord(canonical, child, childAlleles.get(child));
      }
    }
    return result;
  }

  /**
   * Writes the output to the given stream
   * @param app destination for results
   * @throws IOException if an IO error occurs
   */
  public void writeResults(Appendable app) throws IOException {
    long grandTotal = 0;
    for (Map.Entry<Parents, MultiSet<Genotype>> e : mAlleleSets.entrySet()) {
      grandTotal += e.getValue().totalCount();
    }
    final List<Pair<Long, StringBuilder>> results = new ArrayList<>(mAlleleSets.size());
    for (Map.Entry<Parents, MultiSet<Genotype>> e : mAlleleSets.entrySet()) {
      final StringBuilder res = new StringBuilder();
      final Parents p = e.getKey();
      final MultiSet<Genotype> counts = e.getValue();
      final long tot = counts.totalCount();
      final String parentLabel = String.format("%5s %5s", p.mParents[0], p.mParents[1]);

      for (Genotype i : counts.keySet()) {
        res.append(String.format(Locale.ROOT, "%10s %6s %10d  %6.2f%% %6.2f%%", parentLabel, i.toString(), counts.get(i), (double) counts.get(i) * 100.0d / tot, (double) counts.get(i) * 100.0d / grandTotal));
        if (MendeliannessAnnotator.isBadTrioCall(p.mParents[0], p.mParents[1], i)) {
          res.append(" MIE");
        }
        res.append(StringUtils.LS);
      }
      results.add(new Pair<>(tot, res));
    }
    results.sort(new CountPairComparator());

    app.append("   Parental  Child      Count    Local  Global").append(StringUtils.LS);
    for (Pair<Long, StringBuilder> p : results) {
      app.append(p.getB().toString());
      app.append(StringUtils.LS);
    }
  }


  private static class Parents {
    static final ParentsComparator PARENTS_COMPARATOR = new Parents.ParentsComparator();

    Genotype[] mParents;
    Parents(Genotype parent1, Genotype parent2) {
      mParents = new Genotype[] {parent1, parent2};
      Arrays.sort(mParents, new Genotype.GenotypeComparator());
    }

    public boolean equals(Object o) {
      if (o == null || !(o instanceof  Parents)) {
        return false;
      }
      return Arrays.equals(mParents, ((Parents) o).mParents);
    }

    public int hashCode() {
      int hash = 0;
      for (Genotype i : mParents) {
        hash = Utils.pairHash(hash, i.hashCode());
      }
      return hash;
    }

    private static class ParentsComparator implements Comparator<Parents>, Serializable {
      @Override
      public int compare(Parents a, Parents b) {
        int alleleCountA = 0;
        for (Genotype i : a.mParents) {
          alleleCountA += i.length();
        }
        int alleleCountB = 0;
        for (Genotype i : b.mParents) {
          alleleCountB += i.length();
        }
        if (alleleCountA < alleleCountB) {
          return 1;
        } else if (alleleCountA > alleleCountB) {
          return -1;
        }
        assert a.mParents.length == 2 && b.mParents.length == 2;
        for (int i = 0; i < 2; ++i) {
          if (Genotype.GENOTYPE_COMPARATOR.compare(a.mParents[i], b.mParents[i]) < 0) {
            return -1;
          } else if (Genotype.GENOTYPE_COMPARATOR.compare(a.mParents[i], b.mParents[i]) > 0) {
            return 1;
          }
        }
        return 0;
      }
    }
  }

  private static class CountPairComparator implements Comparator<Pair<Long, StringBuilder>>, Serializable {
    @Override
    public int compare(Pair<Long, StringBuilder> a, Pair<Long, StringBuilder> b) {
      return b.getA().compareTo(a.getA());
    }
  }


  private static void useReader(File f) throws IOException {
    final GenotypeProportions prop = new GenotypeProportions();
    try (VcfReader vcfR = VcfReader.openVcfReader(f)) {
      while (vcfR.hasNext()) {
        final VcfRecord rec = vcfR.next();
        final ArrayList<String> sampleGts = rec.getFormat(VcfUtils.FORMAT_GENOTYPE);
        // Assumes samples are already ordered: Father, Mother, Child
        prop.addRecord(new Genotype(sampleGts.get(0)), new Genotype(sampleGts.get(1)), new Genotype(sampleGts.get(2)));
      }
    }
    prop
//      .filterNonDiploid().filterMultiallelic().canonicalParents()
      .writeResults(System.err);
  }

  /**
   * @param args should have length 1 and be the filename of the file to process
   * @throws Exception could be anything
   */
  public static void main(String[] args) throws Exception {
    useReader(new File(args[0]));
  }

}

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

import static com.rtg.launcher.CommonFlags.BOOL;
import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.FLOAT;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.PEDIGREE_FLAG;
import static com.rtg.launcher.CommonFlags.STRING;
import static com.rtg.simulation.variants.ChildSampleSimulatorCli.GENETIC_MAP_DIR;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.Sex;
import com.rtg.relation.Family;
import com.rtg.relation.GenomeRelationships;
import com.rtg.relation.MultiFamilyOrdering;
import com.rtg.relation.PedigreeException;
import com.rtg.relation.Relationship;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.Pair;
import com.rtg.util.PortableRandom;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogStream;
import com.rtg.variant.GenomePriorParams;
import com.rtg.vcf.FilterVcfWriter;
import com.rtg.vcf.StatisticsVcfWriter;
import com.rtg.vcf.VariantStatistics;
import com.rtg.vcf.VcfFilter;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.VcfHeader;

/**
 * Generate the genotypes for all samples in a pedigree file.
 */
public class PedSampleSimulatorCli extends LoggedCli {

  private static final String REMOVE_UNUSED = "remove-unused";
  private static final String REFERENCE_SDF = "reference";
  private static final String SEED = "seed";
  private static final String OUTPUT_SDF = "output-sdf";
  private static final String PLOIDY = "ploidy";
  private static final String PRIORS_FLAG = "Xpriors";
  private static final String TEMP_COMPRESSED_FLAG = "Xtemp-files-gzipped";

  private SampleSimulator mSampleSim;
  private ChildSampleSimulator mChildSim;
  private DeNovoSampleSimulator mDenovoSim;
  private SampleReplayer mSampleReplayer;
  private File mPopVcf = null;
  private File mOutputDir = null;
  private File mCurrentVcf = null;
  private Set<String> mSamples; // All samples present at the current point in time.
  private Collection<String> mCreated; // The samples created by us
  private static final boolean CLEANUP = true;
  private final List<Pair<String, Job>> mJobs = new ArrayList<>();
  private int mJob;
  private int mTotalJobs;

  @Override
  public String moduleName() {
    return "pedsamplesim";
  }

  @Override
  public String description() {
    return "generate simulated genotypes for all members of a pedigree";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Generates simulated genotypes for all members of a pedigree.");
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initForce(mFlags);
    CommonFlags.initReferenceTemplate(mFlags, REFERENCE_SDF, true, "");
    CommonFlags.initOutputDirFlag(mFlags);
    mFlags.registerRequired('i', CommonFlags.INPUT_FLAG, File.class, FILE, "input VCF containing parent variants").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('p', PEDIGREE_FLAG, File.class, FILE, "genome relationships PED file").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_SDF, "if set, output an SDF for the genome of each simulated sample").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(PRIORS_FLAG, String.class, STRING, "selects a properties file specifying the mutation priors. Either a file name or one of [human]", "human").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(PLOIDY, ReferencePloidy.class, STRING, "ploidy to use", ReferencePloidy.AUTO).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(ChildSampleSimulatorCli.EXTRA_CROSSOVERS, Double.class, FLOAT, "probability of extra crossovers per chromosome", ChildSampleSimulatorCli.EXTRA_CROSSOVERS_PER_CHROMOSOME).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(TEMP_COMPRESSED_FLAG, Boolean.class, BOOL, "gzip temporary VCF files", true).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(SEED, Integer.class, CommonFlags.INT, "seed for the random number generator").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(REMOVE_UNUSED, "if set, output only variants used by at least one sample").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(DeNovoSampleSimulatorCli.EXPECTED_MUTATIONS, Integer.class, CommonFlags.INT, "expected number of mutations per genome", DeNovoSampleSimulatorCli.DEFAULT_MUTATIONS_PER_GENOME).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(GENETIC_MAP_DIR, File.class, CommonFlags.DIR, "if set, load genetic maps from this directory for recombination point selection").setCategory(CommonFlagCategories.UTILITY);
    CommonFlags.initNoGzip(mFlags);

    mFlags.setValidator(flags -> CommonFlags.validateSDF(flags, REFERENCE_SDF)
      && CommonFlags.validateTabixedInputFile(flags, CommonFlags.INPUT_FLAG)
      && CommonFlags.validateOutputDirectory(flags)
      && flags.checkInRange(DeNovoSampleSimulatorCli.EXPECTED_MUTATIONS, 0, Integer.MAX_VALUE)
      && flags.checkInRange(ChildSampleSimulatorCli.EXTRA_CROSSOVERS, 0.0, 1.0));
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(OUTPUT_FLAG);
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final CFlags flags = mFlags;
    final PortableRandom random;
    if (flags.isSet(SEED)) {
      random = new PortableRandom((Integer) flags.getValue(SEED));
    } else {
      random = new PortableRandom();
    }
    final GenomePriorParams priors = GenomePriorParams.builder().genomePriors((String) mFlags.getValue(PRIORS_FLAG)).create();
    final GenomeRelationships pedigree = GenomeRelationships.loadGenomeRelationships((File) mFlags.getValue(PEDIGREE_FLAG));
    final List<Family> families;
    try {
      families = MultiFamilyOrdering.orderFamiliesAndSetMates(Family.getFamilies(pedigree, false, null));
    } catch (PedigreeException e) {
      throw new NoTalkbackSlimException(e.getMessage());
    }

    final File reference = (File) flags.getValue(REFERENCE_SDF);
    final File popVcf = (File) flags.getValue(CommonFlags.INPUT_FLAG);
    final ReferencePloidy ploidy = (ReferencePloidy) flags.getValue(PLOIDY);
    try (final SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(reference, true, false, LongRange.NONE)) {
      mSampleSim = new SampleSimulator(dsr, new PortableRandom(random.nextInt()), ploidy, false);
      mSampleSim.mAddRunInfo = false;
      mSampleSim.mDoStatistics = false;
      final CrossoverSelector csel = new CrossoverSelector((File) flags.getValue(GENETIC_MAP_DIR), (Double) flags.getValue(ChildSampleSimulatorCli.EXTRA_CROSSOVERS), false);
      mChildSim = new ChildSampleSimulator(dsr, new PortableRandom(random.nextInt()), ploidy, csel, false);
      mChildSim.mAddRunInfo = false;
      mChildSim.mDoStatistics = false;
      final int deNovoMutations = (Integer) flags.getValue(DeNovoSampleSimulatorCli.EXPECTED_MUTATIONS);
      if (deNovoMutations > 0) {
        mDenovoSim = new DeNovoSampleSimulator(dsr, priors, new PortableRandom(random.nextInt()), ploidy, deNovoMutations, false);
        mDenovoSim.mAddRunInfo = false;
        mDenovoSim.mDoStatistics = false;
      } else {
        mDenovoSim = null;
      }
      mSampleReplayer = mFlags.isSet(OUTPUT_SDF) ? new SampleReplayer(dsr) : null;

      mCreated = new ArrayList<>();
      mSamples = new HashSet<>();
      mSamples.addAll(VcfUtils.getHeader(popVcf).getSampleNames());
      if (!mSamples.isEmpty()) {
        Diagnostic.info("Input VCF already contains samples: " + mSamples);
      }
      mPopVcf = popVcf;
      mOutputDir = outputDirectory();

      // Get the names of all samples we can generate as children using family information
      final Collection<String> children = new HashSet<>();
      for (Family family : families) {
        children.addAll(Arrays.asList(family.getChildren()));
      }
      children.removeAll(mSamples);

      final Collection<String> independents = new TreeSet<>(Arrays.asList(pedigree.genomes()));
      independents.removeAll(mSamples);
      independents.removeAll(children);

      final Collection<String> toCreate = Stream.concat(independents.stream(), children.stream()).collect(Collectors.toCollection(TreeSet::new));
      Diagnostic.userLog("Need to simulate " + toCreate.size() + " samples: " + toCreate);
      if (!independents.isEmpty()) {
        simulateIndependents(pedigree, independents);
      }
      simulateChildren(families);

      mTotalJobs = mJobs.size()
        + (!independents.isEmpty() && mFlags.isSet(REMOVE_UNUSED) ? 1 : 0)
        + (mSampleReplayer == null ? 0 : toCreate.size())
        + 1;

      Diagnostic.progress("Simulation: 0/" + mTotalJobs + " Finished");
      mJob = 0;
      mCurrentVcf = null;
      doJobs();

      if (mSampleReplayer != null) {
        for (String sample : mCreated) {
          Diagnostic.userLog("Generating genome for sample: " + sample);
          mSampleReplayer.replaySample(mCurrentVcf, new File(mOutputDir, sample + ".sdf"), sample);
          Diagnostic.progress("Simulation: " + ++mJob + "/" + mTotalJobs + " Finished");
        }
      }

      File results = mCurrentVcf;
      boolean cleanup = CLEANUP;
      if (mCurrentVcf == null) {
        Diagnostic.warning("No samples were generated!");
        results = mPopVcf; // Pass input VCF through
        cleanup = false;
      }
      Diagnostic.userLog("Creating final output VCF");
      final File resultsIndex = TabixIndexer.indexFileName(results);
      final boolean gzip = !flags.isSet(NO_GZIP);
      final File outputVcf = VcfUtils.getZippedVcfFileName(gzip, new File(mOutputDir, moduleName() + VcfUtils.VCF_SUFFIX));
      final VariantStatistics stats = new VariantStatistics(mOutputDir);
      stats.onlySamples(mCreated.toArray(new String[0]));
      try (VcfReader reader = VcfReader.openVcfReader(results);
           VcfWriter writer = new StatisticsVcfWriter<>(new VcfWriterFactory(mFlags).addRunInfo(true).make(reader.getHeader(), outputVcf), stats)) {
        writer.getHeader().addMetaInformationLine(VcfHeader.META_STRING + "SEED=" + random.getSeed());
        while (reader.hasNext()) {
          writer.write(reader.next());
        }
      }
      if (cleanup && resultsIndex.exists() && !resultsIndex.delete()) {
        throw new IOException("Could not delete intermediate file: " + resultsIndex);
      }
      if (cleanup && !results.delete()) {
        throw new IOException("Could not delete intermediate file: " + results);
      }
      Diagnostic.progress("Simulation: " + ++mJob + "/" + mTotalJobs + " Finished");

      if (mCreated.size() > 0) {
        stats.printStatistics(out);
      }
    }
    return 0;
  }

  private void removeUnusedVariants() {
    addJob("remove-unused", (i, o) -> {
      Diagnostic.userLog("Removing variants not selected by any sample");
      try (VcfReader reader = VcfReader.openVcfReader(i);
           VcfWriter writer = new FilterVcfWriter(new VcfWriterFactory().zip(true).make(reader.getHeader(), o), new VcfFilter() {
             @Override
             public boolean accept(VcfRecord record) {
               final List<String> sampleGts = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
               if (sampleGts != null) {
                 for (String sampleGt : sampleGts) {
                   for (int i : VcfUtils.splitGt(sampleGt)) {
                     if (i > 0) {
                       return true;
                     }
                   }
                 }
               }
               return false;
             }

             @Override
             public void setHeader(VcfHeader header) { }
           })) {
        while (reader.hasNext()) {
          writer.write(reader.next());
        }
      }
    });
  }

  private void simulateIndependents(GenomeRelationships pedigree, Collection<String> independents) {
    final String[] samples = new String[independents.size()];
    final Sex[] sexes = new Sex[independents.size()];
    int j = 0;
    for (String sample : independents) {
      // Warn about cases where one parent was missing (these aren't selected as families above, so the child(ren) aren't using inheritance
      final Relationship[] rel = pedigree.relationships(sample, new Relationship.RelationshipTypeFilter(Relationship.RelationshipType.PARENT_CHILD), new Relationship.SecondInRelationshipFilter(sample));
      if (rel.length != 0) {
        if (rel.length == 2) {
          Diagnostic.warning("Sample " + sample + " is a child of unknown sex, generating as independent individual.");
        } else {
          Diagnostic.warning("Sample " + sample + " is a child of non-complete family, generating as independent individual.");
        }
      }
      mSamples.add(sample);
      mCreated.add(sample);
      samples[j] = sample;
      sexes[j] = pedigree.getSex(sample);
      j++;
    }
    addJob("independents", (i, o) -> {
      for (String sample : samples) {
        Diagnostic.userLog("Simulating variants for sample: " + sample);
      }
      mSampleSim.mutateIndividual(i, o, samples, sexes);
    });

    // Clear out unused population variants before starting to add de novo mutations to reduce site conflicts
    if (mFlags.isSet(REMOVE_UNUSED)) {
      removeUnusedVariants();
    }

    if (mDenovoSim != null) {
      for (final String sample : independents) {
        addJob(sample, (i, o) -> {
          Diagnostic.userLog("Simulating de novo mutations for sample: " + sample);
          mDenovoSim.mutateIndividual(i, o, sample, sample);
        });
      }
    }
  }

  // Work through the known pedigree members. Do them in batches of as many children as possible
  // until we find one that requires a parent that hasn't yet been generated as a child. This is
  // to ensure that the de novo mutation generation for the parents has happened before they
  // generate the child.
  private void simulateChildren(List<Family> families) throws IOException {
    final ArrayList<ChildSampleSimulator.Trio> batch = new ArrayList<>();
    final HashSet<String> batchchildren = new HashSet<>();
    for (Family family : families) {
      final String mother = family.getMother();
      final String father = family.getFather();
      if (batchchildren.contains(mother) || batchchildren.contains(father)) {
        simulateChildrenBatch(batch, batchchildren);
      }
      for (String child : family.getChildren()) {
        if (!mSamples.contains(child)) {
          batch.add(new ChildSampleSimulator.Trio(father, mother, child, family.pedigree().getSex(child)));
          batchchildren.add(child);
          mSamples.add(child);
          mCreated.add(child);
        }
      }
    }
    if (!batch.isEmpty()) {
      simulateChildrenBatch(batch, batchchildren);
    }
  }

  private void simulateChildrenBatch(ArrayList<ChildSampleSimulator.Trio> batch, HashSet<String> batchchildren) {
    final ChildSampleSimulator.Trio[] trios = batch.toArray(new ChildSampleSimulator.Trio[0]);
    addJob("children", (i, o) -> {
      Diagnostic.userLog("Generating a batch of " + trios.length + " children");
      for (ChildSampleSimulator.Trio trio : trios) {
        Diagnostic.userLog("Simulating variant inheritance for sample: " + trio.child());
      }
      mChildSim.mutateIndividual(i, o, trios);
    });
    if (mDenovoSim != null) {
      for (ChildSampleSimulator.Trio trio : batch) {
        final String child = trio.child();
        addJob(child, (i, o) -> {
          Diagnostic.userLog("Simulating de novo mutations for sample: " + child);
          mDenovoSim.mutateIndividual(i, o, child, child);
        });
      }
    }
    batch.clear();
    batchchildren.clear();
  }

  private interface Job {
    void simulate(File inVcf, File outVcf) throws IOException;
  }

  protected void addJob(String label, final Job job) {
    mJobs.add(new Pair<>(label, job));
  }
  protected void doJobs() throws IOException {
    for (Pair<String, Job> p : mJobs) {
      doJob(p.getA(), p.getB());
    }
    mJobs.clear();
  }
  protected void doJob(String label, final Job job) throws IOException {
    final File inVcf = mCurrentVcf == null ? mPopVcf : mCurrentVcf;
    final File nextVcf = File.createTempFile(moduleName() + "-" + label + ".", VcfUtils.VCF_SUFFIX + ((Boolean) mFlags.getValue(TEMP_COMPRESSED_FLAG) ? FileUtils.GZ_SUFFIX : ""), mOutputDir);
    job.simulate(inVcf, nextVcf);
    if (CLEANUP && mCurrentVcf != null) {
      // We have an intermediate file that now needs to be deleted
      final File indexFile = TabixIndexer.indexFileName(mCurrentVcf);
      if (indexFile.exists() && !indexFile.delete()) {
        throw new IOException("Could not delete intermediate file: " + indexFile);
      }
      if (!mCurrentVcf.delete()) {
        throw new IOException("Could not delete intermediate file: " + mCurrentVcf);
      }
    }
    Diagnostic.progress("Simulation: " + ++mJob + "/" + mTotalJobs + " Finished");
    mCurrentVcf = nextVcf;
  }

}

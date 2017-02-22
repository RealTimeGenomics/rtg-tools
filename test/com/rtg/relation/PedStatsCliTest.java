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
package com.rtg.relation;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

/**
 */
public class PedStatsCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new PedStatsCli();
  }

  private GenomeRelationships makeTestPed() {
    final GenomeRelationships genomeRelationships = new GenomeRelationships();
    genomeRelationships.addGenome("father", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    genomeRelationships.addGenome("mother", GenomeRelationships.SEX_FEMALE);
    genomeRelationships.addGenome("child", GenomeRelationships.SEX_MALE).setProperty(GenomeRelationships.DISEASE_PROPERTY, "true");
    genomeRelationships.addParentChild("father", "child");
    genomeRelationships.addParentChild("mother", "child");
    return genomeRelationships;
  }

  public void testFile() throws IOException {
    try (final TestDirectory dir = new TestDirectory("pedstats")) {
      final File relationFile = new File(dir, "relationshipfile.ped");
      final GenomeRelationships ped = makeTestPed();
      FileUtils.stringToFile(PedFileParser.toString(ped), relationFile);

      final String output = checkMainInitOk("--dot", "My Title", "--simple-dot", relationFile.toString());
      mNano.check("pedstats-todot.txt", output);

      final String outputnew = checkMainInitOk("--dot", "My Title", relationFile.toString());
      mNano.check("pedstats-todot-new.txt", outputnew);
    }
  }

  private static final String RESOURCE_DIR = "com/rtg/relation/resources/";

  public void testIds() throws IOException {
    try (final TestDirectory dir = new TestDirectory("pedstats")) {
      final File relationFile = new File(dir, "octet.ped");
      FileUtils.copyResource(RESOURCE_DIR + "octet.ped", relationFile);

      for (String arg : new String[]{"primary-ids", "male-ids", "female-ids", "paternal-ids", "maternal-ids", "founder-ids"}) {
        final String output = checkMainInitOk("--" + arg, relationFile.toString());
        mNano.check("pedstats-" + arg + ".txt", output);
      }
    }
  }

}

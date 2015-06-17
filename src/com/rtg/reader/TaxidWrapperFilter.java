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
package com.rtg.reader;

import java.io.IOException;
import java.util.Collection;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.taxonomy.TaxonomyUtils;
import com.rtg.util.MultiMap;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Facilitates transferring sequences by (short) name from a SdfReaderWrapper to a WriterWrapper.
 */
@TestClass("com.rtg.reader.Sdf2FastaTest")
class TaxidWrapperFilter extends WrapperFilter {

  private final MultiMap<Integer, Long> mTaxToSeqId;

  TaxidWrapperFilter(SdfReaderWrapper reader, WriterWrapper writer) throws IOException {
    super(reader, writer);
    if (reader.isPaired()) {
      throw new NoTalkbackSlimException("Taxonomy is not supported for paired-end SDFs");
    }
    if (!TaxonomyUtils.hasTaxonomyInfo(reader.single())) {
      throw new NoTalkbackSlimException("The supplied SDF does not contain taxonomy information");
    }
    mTaxToSeqId = TaxonomyUtils.loadTaxonomyIdMapping(reader.single());
  }

  @Override
  protected void warnInvalidSequence(String seqid) {
    if (mWarnCount < 5) {
      Diagnostic.warning("No sequence data for taxonomy id " + seqid);
      mWarnCount++;
      if (mWarnCount == 5) {
        Diagnostic.warning("(Only the first 5 messages shown.)");
      }
    } else {
      Diagnostic.userLog("No sequence data for taxonomy id " + seqid);
    }
  }

  /**
   * Transfer an interpreted sequence or set of sequences from the reader to the writer.
   * This implementation interprets the specifier as a short sequence name.
   * @param seqRange the sequence name
   * @throws IOException if there was a problem during writing
   */
  @Override
  protected void transfer(String seqRange) throws IOException {
    final Collection<Long> ids = mTaxToSeqId.get(Integer.parseInt(seqRange));
    if (ids != null) {
      for (Long id : ids) {
        transfer(id);
      }
    } else {
      warnInvalidSequence(seqRange);
    }
  }
}

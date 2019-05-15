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

package com.rtg.vcf;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.header.IdField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Helper class for creating a VcfAnnotator from command line flags.
 */
@TestClass("com.rtg.vcf.VcfSubsetTest")
abstract class VcfAnnotatorFactory<T extends VcfAnnotator> {

  private final CFlags mFlags;

  protected VcfAnnotatorFactory(CFlags flags) {
    mFlags = flags;
  }

  /**
   * Create the VcfAnnotator.
   * @param header the VCF header the annotator will be applied to.
   * @return the annotator, or null if current configuration means no annotator is enabled
   */
  public abstract T make(VcfHeader header);

  protected abstract List<? extends IdField<?>> getHeaderFields(VcfHeader header);

  @SuppressWarnings("rawtypes")
  protected Collection<String> getHeaderIds(VcfHeader header) {
    return getHeaderFields(header).stream().map(IdField::getId).collect(Collectors.toSet());
  }

  protected void additionalChecks(String fieldname, Set<String> ids, VcfHeader header) {
    final Set<String> unknownIds = new LinkedHashSet<>(ids);
    unknownIds.removeAll(getHeaderIds(header));
    if (!unknownIds.isEmpty()) {
      Diagnostic.warning(fieldname + " fields not contained in VCF meta-information: " + StringUtils.join(' ', unknownIds));
    }
  }

  protected abstract T makeAnnotator(Set<String> fieldIdsSet, boolean keep);

  protected abstract T makeRemoveAllAnnotator();

  protected T processFlags(VcfHeader header, String removeFlag, String keepFlag, String removeAllFlag, String fieldname) {
    if (removeAllFlag != null && mFlags.isSet(removeAllFlag)) {
      return makeRemoveAllAnnotator();
    } else {
      if (mFlags.isSet(removeFlag) || mFlags.isSet(keepFlag)) {
        final boolean keep = !mFlags.isSet(removeFlag);
        final Set<String> ids = mFlags.getValues(mFlags.isSet(removeFlag) ? removeFlag : keepFlag).stream().map(f -> (String) f).collect(Collectors.toCollection(LinkedHashSet::new));

        additionalChecks(fieldname, ids, header);

        return makeAnnotator(ids, keep);
      }
    }
    return null;
  }
}

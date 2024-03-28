package com.rtg.vcf.eval;

import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.VcfHeader;

import java.util.ArrayList;
import java.util.List;

public class CombinedRocFilter extends RocFilter {
  private final ArrayList<RocFilter> mRocFilters = new ArrayList<>();

  /**
   * Create a CombinedRocFilter which accepts only records that are accepted by all delegate RocFilters.
   * @param inputRocFilters List of RocFilters matched against to accept variant
   */
  public CombinedRocFilter(List<RocFilter> inputRocFilters, Boolean rescale) {
    super("", rescale);
    final ArrayList<String> names = new ArrayList<>();

    for (RocFilter rocFilter : inputRocFilters) {
      if (rocFilter != RocFilter.ALL) {
        mRocFilters.add(rocFilter);
        names.add(rocFilter.name());
      }
    }

    mName = String.join("+", names);
  }

  @Override
  public void setHeader(VcfHeader header) {
    mRocFilters.forEach(f -> f.setHeader(header));
  }

  @Override
  public boolean requiresGt() {
    return mRocFilters.stream().anyMatch(RocFilter::requiresGt);
  }

  @Override
  public boolean accept(VcfRecord rec, int[] gt) {
    return mRocFilters.stream().allMatch(f -> f.accept(rec, gt));
  }
}

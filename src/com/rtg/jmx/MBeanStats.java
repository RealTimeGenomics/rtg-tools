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
package com.rtg.jmx;

import static com.rtg.jmx.MonUtils.NF0;
import static com.rtg.jmx.MonUtils.NF1;
import static com.rtg.jmx.MonUtils.NF2;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.util.Constants;

/**
 * Output stats from management beans.
 */
@JumbleIgnore
public class MBeanStats implements MonStats {

  private static final Class<?> OPERATING_SYSTEM_MX_BEAN_CLASS;
  static {
    Class<?> c;
    try {
      c = Class.forName("com.sun.management.OperatingSystemMXBean");
    } catch (final ClassNotFoundException e) {
      c = null;
    }
    OPERATING_SYSTEM_MX_BEAN_CLASS = c;
  }


  private static final double GB = Constants.GB;
  private static final double BILLION = 1000 * 1000 * 1000;
  private static final String LS = System.lineSeparator();

  private final MemoryMXBean mMBean;
  private final RuntimeMXBean mRBean;
  private final ThreadMXBean mTBean;
  private final OperatingSystemMXBean mOBean;
  private final List<GarbageCollectorMXBean> mGcBean;

  MBeanStats() {
    mMBean = ManagementFactory.getMemoryMXBean();
    mRBean = ManagementFactory.getRuntimeMXBean();
    mTBean = ManagementFactory.getThreadMXBean();
    mOBean = ManagementFactory.getOperatingSystemMXBean();
    mGcBean = ManagementFactory.getGarbageCollectorMXBeans();
  }

  private long getSunOsValue(String method) {
    try {
      final Method m1 = OPERATING_SYSTEM_MX_BEAN_CLASS.getMethod(method, (Class<?>[]) null);
      return (Long) m1.invoke(mOBean, (Object[]) null);
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (final NoSuchMethodException e) {
      throw new IllegalStateException();
    }
  }

  private double getSunOsDoubleValue(String method) {
    try {
      final Method m1 = OPERATING_SYSTEM_MX_BEAN_CLASS.getMethod(method, (Class<?>[]) null);
      return (Double) m1.invoke(mOBean, (Object[]) null);
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (final NoSuchMethodException e) {
      throw new IllegalStateException();
    }
  }

  @Override
  public void addHeader(Appendable out) throws IOException {
    out.append("# Start-time   = ").append(String.valueOf(new java.util.Date(mRBean.getStartTime()))).append(LS);
    out.append("# Total-procs  = ").append(String.valueOf(mOBean.getAvailableProcessors())).append(LS);
    if (OPERATING_SYSTEM_MX_BEAN_CLASS != null) {
      out.append("# Total-mem    = ").append(NF2.format(getSunOsValue("getTotalPhysicalMemorySize") / GB)).append(" GB").append(LS);
      out.append("# Total-swap   = ").append(NF2.format(getSunOsValue("getTotalSwapSpaceSize") / GB)).append(" GB").append(LS);
    }

    out.append("# Heap-max     = ").append(NF2.format(mMBean.getHeapMemoryUsage().getMax() / GB)).append(" GB").append(LS);
    out.append("# Heap-init    = ").append(NF2.format(mMBean.getHeapMemoryUsage().getInit() / GB)).append(" GB").append(LS);
    out.append("# Nonheap-max  = ").append(NF2.format(mMBean.getNonHeapMemoryUsage().getMax() / GB)).append(" GB").append(LS);
    out.append("# Nonheap-init = ").append(NF2.format(mMBean.getNonHeapMemoryUsage().getInit() / GB)).append(" GB").append(LS);

    for (int i = 0; i < mGcBean.size(); i++) {
      final GarbageCollectorMXBean gc = mGcBean.get(i);
      out.append("# GC-").append(String.valueOf(i)).append("         = ").append(gc.getName()).append(LS);
    }
  }

  @Override
  public void addColumnLabelsTop(Appendable out) throws IOException {
    out.append(" ---Up");
    if (OPERATING_SYSTEM_MX_BEAN_CLASS != null) {
      out.append(" ------OS-mem-----");
    }
    out.append(" ---Heap---- -Non-heap--");

    for (int i = 0; i < mGcBean.size(); i++) {
      out.append(" ---GC-").append(String.valueOf(i)).append("----");
    }

    out.append(" -Thrd");
    if (OPERATING_SYSTEM_MX_BEAN_CLASS != null) {
      out.append(" -----CPU-----");
      out.append(" ---OS-CPU---");
    } else {
      out.append(" ---OS");
    }
  }

  @Override
  public void addColumnLabelsBottom(Appendable out) throws IOException {
    out.append("  secs");
    if (OPERATING_SYSTEM_MX_BEAN_CLASS != null) {
      out.append("  comm   mem  swap");
    }
    out.append("  comm  used  comm  used");

    for (int i = 0; i < mGcBean.size(); i++) {
      out.append(" count  time");
    }

    out.append(" count");
    if (OPERATING_SYSTEM_MX_BEAN_CLASS != null) {
      out.append("   time");
      out.append("   cpu%");
      out.append("   cpu%");
    }
    out.append("  load");
  }

  @Override
  public void addColumnData(Appendable out) throws IOException {
    final int width = 5;
    out.append(" ");
    MonUtils.pad(out, "" + mRBean.getUptime() / 1000, 5); // Seconds

    if (OPERATING_SYSTEM_MX_BEAN_CLASS != null) {
      out.append(" ");
      MonUtils.pad(out, NF1.format(getSunOsValue("getCommittedVirtualMemorySize") / GB), width);
      out.append(" ");
      MonUtils.pad(out, NF1.format(getSunOsValue("getFreePhysicalMemorySize") / GB), width);
      out.append(" ");
      MonUtils.pad(out, NF1.format(getSunOsValue("getFreeSwapSpaceSize") / GB), width);
    }

    out.append(" ");
    MonUtils.pad(out, NF1.format(mMBean.getHeapMemoryUsage().getCommitted() / GB), width);
    out.append(" ");
    MonUtils.pad(out, NF1.format(mMBean.getHeapMemoryUsage().getUsed() / GB), width);
    out.append(" ");
    MonUtils.pad(out, NF1.format(mMBean.getNonHeapMemoryUsage().getCommitted() / GB), width);
    out.append(" ");
    MonUtils.pad(out, NF1.format(mMBean.getNonHeapMemoryUsage().getUsed() / GB), width);

    for (GarbageCollectorMXBean gc : mGcBean) {
      out.append(" ");
      MonUtils.pad(out, "" + gc.getCollectionCount(), width);
      out.append(" ");
      MonUtils.pad(out, "" + gc.getCollectionTime() / 1000, width);
    }

    out.append(" ");
    MonUtils.pad(out, "" + mTBean.getThreadCount(), width);

    if (OPERATING_SYSTEM_MX_BEAN_CLASS != null) {
      out.append(" ");
      MonUtils.pad(out, NF0.format(getSunOsValue("getProcessCpuTime") / BILLION), width + 1);
      out.append(" ");
      MonUtils.pad(out, NF1.format(getSunOsDoubleValue("getProcessCpuLoad") * 100), width + 1);
      out.append(" ");
      MonUtils.pad(out, NF1.format(getSunOsDoubleValue("getSystemCpuLoad") * 100), width + 1);
    }

    out.append(" ");
    MonUtils.pad(out, NF1.format(mOBean.getSystemLoadAverage()), width);

  }
}

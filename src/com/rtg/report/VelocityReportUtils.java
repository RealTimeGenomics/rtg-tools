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
package com.rtg.report;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.rtg.util.HtmlReportHelper;

/**
 * tools for template reports using Velocity engine
 */
public final class VelocityReportUtils {
  private VelocityReportUtils() { }

  private static final String DEFAULT_TEMPLATE = "default.vm";

  static final String TEMPLATE_DIR = "com/rtg/report/resources";
  static {
    Velocity.setProperty(VelocityEngine.RESOURCE_LOADER, "class");
    Velocity.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getCanonicalName());
    Velocity.setProperty("class.resource.loader.path", "/");
    Velocity.setProperty("runtime.log.logsystem.class", RtgVelocityLogChute.class.getCanonicalName());
    Velocity.init();
  }

  /**
   * Wrap the default RTG template around some body text.
   * @param bodyText the body text
   * @param title title of page/report
   * @param hrh a HTML resources helper
   * @return the processed report
   * @throws IOException if an IO error occurs
   */
  public static String wrapDefaultTemplate(String bodyText, String title, HtmlReportHelper hrh) throws IOException {
    final Map<String, String> data = new HashMap<>();
    data.put("body", bodyText);
    data.put("title", title);
    data.put("resourceDir", hrh.getResourcesDirName());
    hrh.copyResources(TEMPLATE_DIR + "/rtg.css", TEMPLATE_DIR + "/rtg_logo.png", TEMPLATE_DIR + "/table.css");
    return processTemplate(DEFAULT_TEMPLATE, data);
  }

  /**
   * process a template, producing final output
   * @param template template for report
   * @param replacements data for report
   * @return the processed report
   * @throws IOException if an IO error occurs
   */
  public static String processTemplate(String template, Map<String, ?> replacements) throws IOException {
    final VelocityContext vc = new VelocityContext();
    for (Map.Entry<String, ?> me : replacements.entrySet()) {
      vc.put(me.getKey(), me.getValue());
    }
    final String ret;
    try (final StringWriter sw = new StringWriter()) {
      Velocity.getTemplate(TEMPLATE_DIR + "/" + template).merge(vc, sw);
      sw.flush();
      ret = sw.toString();
    }
    return ret;
  }
}

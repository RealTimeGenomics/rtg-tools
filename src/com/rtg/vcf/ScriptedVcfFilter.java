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
package com.rtg.vcf;

import java.io.InputStreamReader;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.rtg.util.Resources;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.VcfHeader;

/**
 * Filter than runs supplied Javascript to determine if record should be accepted
 */
public class ScriptedVcfFilter implements VcfFilter {
  private final String mScript;
  private final ScriptEngine mEngine;
  private final CompiledScript mCompile;
  private VcfHeader mHeader;

  /**
   * @param expression script to run to determine if record should be accepted
   */
  public ScriptedVcfFilter(String expression) {
    mScript = expression;
    final ScriptEngineManager manager = new ScriptEngineManager();
    try {
      mEngine = manager.getEngineByName("js");
      if (mEngine == null || !(mEngine instanceof Compilable)) {
        throw new NoTalkbackSlimException("No compatible javascript engine available");
      }
      final Compilable compileable = (Compilable) mEngine;
      mCompile = compileable.compile(expression);
    } catch (ScriptException e) {
      throw new NoTalkbackSlimException("Could not evaluate the provided expression" + e.getMessage());
    }
  }

  @Override
  public boolean accept(VcfRecord record) {
    try {
      mEngine.put("rec", record);
      return Boolean.valueOf(mCompile.eval().toString());
    } catch (ScriptException e) {
      throw new NoTalkbackSlimException("Could not evaluate script on record: " + record + " |" + e.getMessage());
    }
  }

  @Override
  public void setHeader(VcfHeader header) {
    mHeader = header;
    mEngine.put("header", mHeader);
    try {
      mEngine.eval(new InputStreamReader(Resources.getResourceAsStream("com/rtg/vcf/resources/vcf_filter_preamble.js")));
    } catch (ScriptException e) {
      throw new NoTalkbackSlimException("Could not evaluate the content of preamble." + e.getMessage());
    }
  }
}

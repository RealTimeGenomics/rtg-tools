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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.VcfHeader;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Undefined;

/**
 * Filter than runs supplied Javascript to determine if record should be accepted
 */
public class ScriptedVcfFilter implements VcfFilter {
  /** Name of the end function */
  public static final String END = "end";
  /** Name of the record function */
  private static final String RECORD = "record";
  private static final String RECORD_VARIABLE = "RTG_VCF_RECORD";
  private static final String HEADER_VARIABLE = "RTG_VCF_HEADER";

  private final ScriptEngine mEngine;
  private final CompiledScript mCompiledExpression;
  private final List<CompiledScript> mBeginnings = new ArrayList<>();
  private ScriptObjectMirror mRecordFunction;


  /**
   * @param expression script to run to determine if record should be accepted
   * @param beginnings initialisation scripts to run at start of processing
   * @param output output stream for the JavaScript print method
   */
  public ScriptedVcfFilter(String expression, List<String> beginnings, OutputStream output) {
    final ScriptEngineManager manager = new ScriptEngineManager();
    mEngine = manager.getEngineByName("js");
    if (mEngine == null || !(mEngine instanceof Compilable)) {
      throw new NoTalkbackSlimException("No compatible javascript engine available");
    }
    mEngine.getContext().setWriter(new OutputStreamWriter(output));
    final Compilable compileable = (Compilable) mEngine;
    if (expression == null) {
      mCompiledExpression = null;
    } else {
      try {
        mCompiledExpression = compileable.compile(expression);
      } catch (ScriptException e) {
        throw new NoTalkbackSlimException("Could not evaluate the provided expression" + StringUtils.LS + e.getMessage());
      }
    }
    for (String beginning : beginnings) {
      try {
        mBeginnings.add(compileable.compile(beginning));
      } catch (ScriptException e) {
        throw new NoTalkbackSlimException("Could not evaluate the beginning expression" + StringUtils.LS + e.getMessage());
      }
    }
  }

  @Override
  public boolean accept(VcfRecord record) {
    if (mCompiledExpression == null && mRecordFunction == null) {
      return true;
    }
    mEngine.put(RECORD_VARIABLE, record);
    return invokeExpression(record) && invokeRecordFunction(record);
  }

  private Boolean invokeExpression(VcfRecord record) {
    if (mCompiledExpression == null) {
      return true;
    }
    try {
      final Object eval = mCompiledExpression.eval();
      if (eval == null || !(eval instanceof Boolean)) {
        throw new NoTalkbackSlimException("Could not evaluate script on record: " + record + StringUtils.LS + "The expression did not evaluate to a boolean value.");
      }
      return Boolean.valueOf(eval.toString());
    } catch (ScriptException e) {
      throw new NoTalkbackSlimException("Could not evaluate script on record: " + record + StringUtils.LS + e.getMessage());
    }
  }

  private boolean invokeRecordFunction(VcfRecord record) {
    if (mRecordFunction == null) {
      return true;
    }
    try {
      final Object o = mRecordFunction.call(null);
      if (o instanceof Undefined) {
        return true;
      }
      if (!(o instanceof Boolean)) {
        throw new NoTalkbackSlimException("Could not evaluate script on record: " + record + StringUtils.LS + "The return value of the record function was not a boolean.");
      }
      return (Boolean) o;
    } catch (RuntimeException e) {
      throw new NoTalkbackSlimException("Can't evaluate record function against : " + record + StringUtils.LS + e.getMessage());
    }
  }

  @Override
  public void setHeader(VcfHeader header) {
    mEngine.put(HEADER_VARIABLE, header);
    try {
      mEngine.eval(new InputStreamReader(Resources.getResourceAsStream("com/rtg/vcf/resources/vcf_filter_preamble.js")));
    } catch (ScriptException e) {
      throw new NoTalkbackSlimException("Error evaluating preamble" + StringUtils.LS + e.getMessage());
    }
    for (CompiledScript begin : mBeginnings) {
      try {
        begin.eval();
      } catch (ScriptException e) {
        throw new NoTalkbackSlimException("Error running begin script" + StringUtils.LS + e.getMessage());
      }
    }
    mRecordFunction = getFunction(RECORD);
  }

  private ScriptObjectMirror getFunction(String functionName) {
    final Object function = mEngine.get(functionName);
    if (function == null || !(function instanceof ScriptObjectMirror)) {
      return null;
    }
    final ScriptObjectMirror mirror = (ScriptObjectMirror) function;
    return mirror.isFunction() ? mirror : null;
  }

  /**
   * Invoke at the end of processing to run the ending scripts
   */
  public void end() {
    final ScriptObjectMirror end = getFunction(END);
    if (end != null) {
      try {
        end.call(null);
      } catch (RuntimeException e) {
        throw new NoTalkbackSlimException("Can't evaluate end function: " + StringUtils.LS + e.getMessage());
      }
    }
  }

}

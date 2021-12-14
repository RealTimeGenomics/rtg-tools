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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.Environment;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.header.VcfHeader;

/**
 * Class for running supplied Javascript expressions/scripts against VCF records.
 */
@TestClass("com.rtg.vcf.ScriptedVcfFilterTest")
public class ScriptedVcfProcessor {

  /** Name of the record function */
  protected static final String RECORD = "record";
  protected static final String RECORD_VARIABLE = "RTG_VCF_RECORD";
  protected static final String HEADER_VARIABLE = "RTG_VCF_HEADER";
  protected static final String VERSION_VARIABLE = "RTG_VERSION";

  protected final ScriptEngine mEngine;
  protected final CompiledScript mCompiledExpression;
  protected final List<CompiledScript> mBeginnings = new ArrayList<>();
  protected boolean mHasRecordFunction;
  private VcfRecord mRecord;


  /**
   * @param expression script to run to determine if record should be accepted
   * @param beginnings initialisation scripts to run at start of processing
   * @param output output stream for the JavaScript print method
   * @param err output stream for our JavaScript error method
   */
  public ScriptedVcfProcessor(String expression, List<String> beginnings, OutputStream output, PrintStream err) {
    final ScriptEngineManager manager = new ScriptEngineManager();
    mEngine = manager.getEngineByName("js");
    if (mEngine == null) {
      throw new NoTalkbackSlimException("JVM does not contain a javascript engine");
    }
    if (!(mEngine instanceof Compilable)) {
      throw new NoTalkbackSlimException("Javascript engine is not compilable");
    }
    if (!(mEngine instanceof Invocable)) {
      throw new NoTalkbackSlimException("Javascript engine is not invokable");
    }
//    Diagnostic.userLog("Script Engine: " + mEngine.getFactory().getEngineName()
//      + " " + mEngine.getFactory().getLanguageName()
//      + " (" + mEngine.getFactory().getLanguageVersion() + ")");
    final ScriptContext sc = mEngine.getContext();
    sc.setWriter(new OutputStreamWriter(output));
    sc.setErrorWriter(new PrintWriter(err));
    sc.setAttribute("stderr", // Make an attribute 'stderr' that we can send Strings to from JS
      (Consumer<String>) str -> {
        try {
          final Writer errDest = sc.getErrorWriter();
          errDest.write(str);
          errDest.write(StringUtils.LS);
          errDest.flush();
        } catch (Exception e) {
          throw new Error(e);
        }
      },
      ScriptContext.ENGINE_SCOPE
    );
    mEngine.put(VERSION_VARIABLE, Environment.getProductVersion());
    final Compilable compileable = (Compilable) mEngine;
    if (expression == null) {
      mCompiledExpression = null;
    } else {
      try {
        mCompiledExpression = compileable.compile(expression);
      } catch (ScriptException | AssertionError e) {
        throw new NoTalkbackSlimException("Could not evaluate the provided expression" + StringUtils.LS + e.getMessage());
      }
    }
    for (String beginning : beginnings) {
      try {
        mBeginnings.add(compileable.compile(beginning));
      } catch (ScriptException | AssertionError e) {
        throw new NoTalkbackSlimException("Could not evaluate the beginning expression" + StringUtils.LS + e.getMessage());
      }
    }
  }

  /**
   * Set the VCF header to be used with this expression processor. This should be called once per VCF file.
   * @param header VCF header
   */
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
    try {
      mHasRecordFunction = hasFunction(RECORD);
    } catch (ScriptException e) {
      throw new NoTalkbackSlimException("Error determining if script contains record function" + StringUtils.LS + e.getMessage());
    }
  }

  /**
   * Set the current VCF record to be accessed by any per-record functions.
   * @param record VCF record
   */
  public void setRecord(VcfRecord record) {
    mRecord = record;
    mEngine.put(RECORD_VARIABLE, record);
  }

  /**
   * Evaluates the compiled expression and returns the result.
   * @return the result of the expression
   */
  public Object invokeExpression() {
    try {
      return mCompiledExpression.eval();
    } catch (ScriptException e) {
      throw new NoTalkbackSlimException("Could not evaluate script on record: " + mRecord + StringUtils.LS + e.getMessage());
    }
  }

  /**
   * Evaluates the defined record function and returns the result.
   * @return the result of evaluating the record function.
   */
  public Object invokeRecordFunction() {
    try {
      return ((Invocable) mEngine).invokeFunction(RECORD);
    } catch (RuntimeException | ScriptException | NoSuchMethodException e) {
      throw new NoTalkbackSlimException("Can't evaluate record function against : " + mRecord + StringUtils.LS + e.getMessage());
    }
  }

  protected boolean hasFunction(String name) throws ScriptException {
    return (Boolean) mEngine.eval("typeof " + name + " === 'function' ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE");
  }
}

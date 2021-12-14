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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptException;

import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Filter than runs supplied Javascript to determine if record should be accepted
 */
public class ScriptedVcfFilter extends ScriptedVcfProcessor implements VcfFilter {

  /** Name of the end function */
  protected static final String END = "end";

  /**
   * @param expression script to run to determine if record should be accepted
   * @param beginnings initialisation scripts to run at start of processing
   * @param output output stream for the JavaScript print method
   * @param err output stream for our JavaScript error method
   */
  public ScriptedVcfFilter(String expression, List<String> beginnings, OutputStream output, PrintStream err) {
    super(expression, beginnings, output, err);
  }

  @Override
  public boolean accept(VcfRecord record) {
    setRecord(record);
    if (mCompiledExpression != null) {
      final Object o = invokeExpression();
      if (!(o instanceof Boolean)) {
        throw new NoTalkbackSlimException("Could not evaluate script on record: " + record + StringUtils.LS + "The expression did not evaluate to a boolean value.");
      }
      if (!(Boolean) o) {
        return false;
      }
    }
    if (mHasRecordFunction) {
      final Object o = invokeRecordFunction();
      if (o == null) {
          return true;
        }
      if (!(o instanceof Boolean)) {
        throw new NoTalkbackSlimException("Could not evaluate script on record: " + record + StringUtils.LS + "The return value of the record function was not a boolean.");
      }
      if (!(Boolean) o) {
        return false;
      }
    }
    return true;
  }


  /**
   * Invoke at the end of processing to run the ending scripts
   */
  public void end() {
    try {
      if (hasFunction(END)) {
        ((Invocable) mEngine).invokeFunction(END);
      }
    } catch (RuntimeException | ScriptException | NoSuchMethodException e) {
      throw new NoTalkbackSlimException("Can't evaluate end function: " + StringUtils.LS + e.getMessage());
    }
  }
}

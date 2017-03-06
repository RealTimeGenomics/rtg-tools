/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.util.cli;

import java.util.ArrayList;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Switch is an untyped flag, where no value needs to be specified by the user. 
 */
@TestClass(value = {"com.rtg.util.cli.CFlagsTest"})
public class Switch extends Flag<Boolean> {

  /**
   * Constructor for a <code>Switch</code>. These are boolean valued
   * flags where the value does not need to be supplied -- if the
   * flag is specified the value is true, otherwise the flag is
   * false.
   * 
   * @param flagChar a <code>Character</code> which can be supplied by the
   * user as an abbreviation for flagName. May be null.
   * @param flagName a <code>String</code> which is the name that the user
   * specifies on the command line to denote the flag.
   * @param flagDescription a description of the meaning of the flag.
   */
  public Switch(Character flagChar, final String flagName, final String flagDescription) {
    super(flagChar, flagName, flagDescription, 0, 1, Boolean.class, null, null, null);
    if (flagName == null) {
      throw new IllegalArgumentException();
    }
    setParameterDescription(null);
  }

  @Override
  public Class<Boolean> getParameterType() {
    return null;
  }

  @Override
  protected Boolean parseValue(final String valueStr) {
    return Boolean.TRUE;
  }

  @Override
  public Switch setParameterRange(String[] range) {
    throw new IllegalArgumentException("Cannot set parameter range for no-arg flags.");
  }

  @Override
  public Boolean getValue() {
    return isSet();
  }

  @Override
  public List<Boolean> getValues() {
    if (isSet()) {
      return super.getValues();
    } else {
      final List<Boolean> result = new ArrayList<Boolean>();
      result.add(Boolean.FALSE);
      return result;
    }
  }
}

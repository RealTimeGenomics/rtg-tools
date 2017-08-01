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
package com.rtg.util.cli;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import org.junit.Assert;

import com.reeltwo.spelling.Spelling;
import com.rtg.util.StringUtils;

/**
 * Static access to spelling checking system.
 */
public final class CheckSpelling {

  static Spelling sSpelling = null;

  private CheckSpelling() { }

  static void setSpelling(final StringBuilder problems) throws IOException {
    sSpelling = new Spelling() {
        {
          includeStandardDictionaries();
          addCaseInsensitiveDictionary("com/rtg/util/cli/spell.insensitive");
          addCaseSensitiveDictionary("com/rtg/util/cli/spell.sensitive");
        }

        @Override
        protected void warning(final String source, final int lineNumber, final String msg) {
          problems.append("Source: ").append(source).append(" ").append(msg).append(StringUtils.LS);
        }
      };
  }

  static void check(final String name, final String s) {
    try {
      try (LineNumberReader r = new LineNumberReader(new StringReader(s))) {
        sSpelling.checkText(name, r);
      }
    } catch (final IOException e) {
      Assert.fail(e.getMessage());
    }
  }

}


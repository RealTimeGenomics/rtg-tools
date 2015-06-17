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
package com.rtg.util;

import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Validator;


/**
 * Class to choose a percentage of the available RAM for use in the RTG wrapper script.
 */
public final class ChooseMemory {

  private ChooseMemory() { }

  /**
   * Main method for getting RAM.
   * @param args the command line arguments.
   */
  public static void main(String[] args) {
    final CFlags flags = new CFlags("ChooseMemory", "Program to get the appropriate RAM to use", System.out, System.err);
    flags.registerRequired(Integer.class, "INT", "Percentage of RAM to use (1-100)");
    flags.setValidator(new Validator() {
      @Override
      public boolean isValid(CFlags flags) {
        final int percentage = (Integer) flags.getAnonymousValue(0);
        if (percentage < 1) {
          flags.setParseMessage("Percentage must be greater than 0.");
          return false;
        } else if (percentage > 100) {
          flags.setParseMessage("Percentage must be less than or equal to 100.");
          return false;
        }
        return true;
      }
    });
    if (!flags.setFlags(args)) {
      return;
    }

    final double percentage = ((Integer) flags.getAnonymousValue(0)) / 100.0;
    final int megs = (int) (Environment.getTotalMemory() * percentage / 1024.0 / 1024.0);
    System.out.println((megs < 1024 ? 1024 : megs) + "m");
  }
}

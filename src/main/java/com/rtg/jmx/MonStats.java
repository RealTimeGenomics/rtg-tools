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

import java.io.IOException;

/**
 * Interface for objects adding monitor stats to output.
 */
public interface MonStats {

  /**
   * Output any data to be produced once at the start of monitoring.
   *
   * @param out the destination <code>Appendable</code>
   * @throws IOException if there is a problem during output
   */
  void addHeader(Appendable out) throws IOException;

  /**
   * Output tab separated column labels corresponding to data produced by this object.
   *
   * @param out the destination <code>Appendable</code>
   * @throws IOException if there is a problem during output
   */
  void addColumnLabelsTop(Appendable out) throws IOException;

  /**
   * Output tab separated column labels corresponding to data produced by this object.
   *
   * @param out the destination <code>Appendable</code>
   * @throws IOException if there is a problem during output
   */
  void addColumnLabelsBottom(Appendable out) throws IOException;

  /**
   * Output tab separated column data.
   *
   * @param out the destination <code>Appendable</code>
   * @throws IOException if there is a problem during output
   */
  void addColumnData(Appendable out) throws IOException;

}

/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.simulation.reads;

import java.io.IOException;

import com.rtg.reader.PrereadType;
import com.rtg.reader.SdfId;
import com.rtg.util.machine.MachineType;

/**
 * Interface to represent different machines for read simulation.
 */
public interface Machine {

  /**
   * Sets the minimum and maximum quality values that will be output for bases.
   *
   * @param minq the minimum quality value permitted.
   * @param maxq the maximum quality value permitted.
   */
  void setQualRange(byte minq, byte maxq);

  /**
   * Sets the destination <code>ReadWriter</code> to which the simulated reads will be sent.
   *
   * @param rw a <code>ReadWriter</code> value
   */
  void setReadWriter(ReadWriter rw);

  /**
   * Specifies the list of template sets used during generation.
   * @param templateIds an array containing an ID for each template set
   */
  void identifyTemplateSet(SdfId... templateIds);

  /**
   * Specifies the original reference template used during mutated genome generation.
   * @param referenceId the ID of the original reference template.
   */
  void identifyOriginalReference(SdfId referenceId);

  /**
   * Take a fragment and generate a read.
   * @param id fragment name
   * @param fragmentStart 0 based start position of fragment within sequence. Negative values are valid, but shouldn't be used for any calculations within this method..
   * @param data residues
   * @param length amount of data that is valid
   * @throws IOException guess
   */
  void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException;

  /**
   * Total residues emitted in generated reads
   * @return the number
   */
  long residues();


  /**
   * @return true if this machine produces paired end data.
   */
  boolean isPaired();

  /**
   * @return type to use when writing generated reads to SDF
   */
  PrereadType prereadType();

  /**
   * @return which type of machine is being simulated.
   */
  MachineType machineType();

  /**
   * @return a textual representation summary of the actions histogram
   */
  String formatActionsHistogram();
}

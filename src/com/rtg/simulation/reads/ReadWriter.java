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

import java.io.Closeable;
import java.io.IOException;

import com.rtg.reader.SdfId;

/**
 * Writes/processes reads generated from a {@link Machine}
 */
public interface ReadWriter extends Closeable {

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
   * Write a single end read
   * @param name name of read
   * @param data sequence data for read
   * @param qual quality data for read
   * @param length length of read
   * @throws IOException whenever
   */
  void writeRead(String name, byte[] data, byte[] qual, int length) throws IOException;

  /**
   * Write left end of paired end read
   * @param name name of read
   * @param data sequence data for read
   * @param qual quality data for read
   * @param length length of read
   * @throws IOException whenever
   */
  void writeLeftRead(String name, byte[] data, byte[] qual, int length) throws IOException;

  /**
   * Write right end of paired end read
   * @param name name of read
   * @param data sequence data for read
   * @param qual quality data for read
   * @param length length of read
   * @throws IOException whenever
   */
  void writeRightRead(String name, byte[] data, byte[] qual, int length) throws IOException;

  /**
   * Returns a count of the reads written by this ReadWriter
   * @return the count of reads written
   */
  int readsWritten();
}

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
package com.rtg.util.arithcode;

/**
 * <P>Interface for an adaptive statistical model of a stream to be used
 * as a basis for arithmetic coding and decoding. As in {@link
 * java.io.InputStream}, bytes are coded as integers in the range
 * <code>0</code> to <code>255</code> and <code>EOF</code> is provided
 * as a constant and coded as <code>-1</code>.  In addition,
 * arithmetic coding requires an integer <code>ESCAPE</code> to code
 * information about the model structure.
 *
 * <P> During encoding, a series of calls will be made to
 * <code>escaped(symbol)</code> where <code>symbol</code> is a byte
 * encoded as an integer in the range 0 to 255 or <code>EOF</code>,
 * and if the result is <code>true</code>, a call to
 * <code>interval(ESCAPE)</code> will be made and the process repeated
 * until a call to <code>escaped(symbol)</code> returns
 * <code>false</code>, at which point a call to
 * <code>interval(symbol)</code> is made and the underlying model is
 * updated.
 *
 * <P> During decoding, a call to <code>total()</code> will be made
 * and then a call to <code>pointToSymbol(count)</code>.  If the
 * result is <code>ESCAPE</code>, the process is repeated.  If the
 * result is a byte encoded as an integer in the range <code>0</code>
 * to <code>255</code> or <code>EOF</code>, the symbol is returned and
 * the underlying model is updated.
 *
 * <P>The probability model required for arithmetic coding is
 * cumulative.  For each outcome, rather than returning a probability,
 * an interval is provided to the coder.  As is usual for arithmetic
 * coding, an interval in <code>[0,1]</code> is represented by three
 * integers, where a low count, a high count, and a total count pick
 * out the interval <code>[low/total,high/total)</code>.
 *
 * <P> For more details, see <a href="../../../tutorial.html">The
 * Arithmetic Coding Tutorial</a>.
 *
 * @version 1.1
 */
public interface ArithCodeModel {

  /**
   * Use the model to encode one symbol into the encoder.
   * @param encoder used to do encoding.
   * @param symbol to be encoded.
   */
  void encode(ArithEncoder encoder, int symbol);

  /**
   * Use the model to decode a single symbol from the decoder.
   * @param decoder used to do decoding.
   * @return the symbol.
   */
  int decode(ArithDecoder decoder);
}

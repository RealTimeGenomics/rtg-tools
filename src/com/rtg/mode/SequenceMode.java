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
package com.rtg.mode;

import static com.rtg.mode.SequenceType.DNA;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.rtg.util.PseudoEnum;

/**
 * The ways sequences can be translated (or not).
 */
public abstract class SequenceMode implements Serializable, PseudoEnum {

  private static final int NUMBER_TRANSLATED_FRAMES = 6;

  private static final int NUMBER_BIDIRECTIONAL_FRAMES = 2;

  /**
   * A protein sequence thus cannot be translated.
   * Internal and external sequence ids are identical.
   */
  public static final SequenceMode PROTEIN = new SequenceMode(0, "PROTEIN", SequenceType.PROTEIN, SequenceType.PROTEIN, 1, 1) {
    @Override
    public Frame frameFromCode(final int value) { //value ignored
      return ProteinFrame.PROTEIN;
    }

    @Override
    public Frame frame(final int internalId) {
      assert internalId >= 0;
      return ProteinFrame.PROTEIN;
    }

    @Override
    public int internalId(final long id, final long offset, final Frame frame) {
      assert offset >= 0;
      final long res = id - offset;
      assert res >= 0 && res <= Integer.MAX_VALUE;
      return (int) res;
    }

    @Override
    public long sequenceId(final int internalId, final long offset) {
      assert internalId >= 0;
      assert offset >= 0;
      return internalId + offset;
    }

    @Override
    public Frame[] allFrames() {
      return ProteinFrame.values();
    }

  };

  /**
   * A nucleotide sequence that is not translated but does have forward and
   * reverse frames.
   */
  public static final SequenceMode BIDIRECTIONAL = new SequenceMode(1, "BIDIRECTIONAL", DNA, DNA, NUMBER_BIDIRECTIONAL_FRAMES, 1) {
    @Override
    public Frame frameFromCode(final int value) {
      return BidirectionalFrame.frameFromOrdinal(value);
    }

    @Override
    public Frame frame(final int internalId) {
      final int mod = internalId % NUMBER_BIDIRECTIONAL_FRAMES;
      return BidirectionalFrame.frameFromOrdinal(mod);
    }

    @Override
    public int internalId(final long id, final long offset, final Frame frame) {
      assert offset >= 0;
      final long idoff = id - offset;
      final long idx = idoff * NUMBER_BIDIRECTIONAL_FRAMES + frame.ordinal();
      if  (idx < 0 || idx > Integer.MAX_VALUE) {
        throw new RuntimeException("Internal id out of range. id=" + id + " frame=" + frame + " internal id=" + idx);
      }
      return (int) idx;
    }

    @Override
    public long sequenceId(final int internalId, final long offset) {
      assert internalId >= 0;
      final int div = internalId / NUMBER_BIDIRECTIONAL_FRAMES;
      assert offset >= 0;
      return div + offset;
    }

    @Override
    public Frame[] allFrames() {
      return BidirectionalFrame.values();
    }

  };

  /**
   * A nucleotide sequence that is not translated and only has a forward frame.
   */
  public static final SequenceMode UNIDIRECTIONAL
  = new SequenceMode(2, "UNIDIRECTIONAL", DNA, DNA, 1, 1) {
    @Override
    public Frame frameFromCode(final int value) {
      return UnidirectionalFrame.frameFromCode(value);
    }

    @Override
    public Frame frame(final int internalId) {
      return UnidirectionalFrame.FORWARD;
    }

    @Override
    public int internalId(final long id, final long offset, final Frame frame) {
      assert offset >= 0;
      final long idoff = id - offset;
      assert idoff >= 0 && idoff <= Integer.MAX_VALUE;
      return (int) idoff;
    }

    @Override
    public long sequenceId(final int internalId, final long offset) {
      assert internalId >= 0;
      assert offset >= 0;
      return internalId + offset;
    }

    @Override
    public Frame[] allFrames() {
      return UnidirectionalFrame.values();
    }
  };

  /**
   * A nucleotide sequence that is translated to all 6 possible frames.
   */
  public static final SequenceMode TRANSLATED
  = new SequenceMode(3, "TRANSLATED", DNA, SequenceType.PROTEIN, NUMBER_TRANSLATED_FRAMES, 3) {
    @Override
    public Frame frameFromCode(final int value) {
      return TranslatedFrame.frameFromCode(value);
    }

    @Override
    public Frame frame(final int internalId) {
      final int mod = internalId % NUMBER_TRANSLATED_FRAMES;
      return TranslatedFrame.frameFromCode(mod);
    }

    @Override
    public int internalId(final long id, final long offset, final Frame frame) {
      assert offset >= 0;
      final long idoff = id - offset;
      assert idoff >= 0 && idoff <= Integer.MAX_VALUE;
      final long idx = idoff * NUMBER_TRANSLATED_FRAMES + frame.ordinal();
      if  (idx < 0 || idx > Integer.MAX_VALUE) {
        throw new RuntimeException("Internal id out of range. id=" + id + " frame=" + frame + " frame ord=" + frame.ordinal() + " internal id=" + idx);
      }
      return (int) idx;
    }

    @Override
    public long sequenceId(final int internalId, final long offset) {
      assert internalId >= 0;
      final int div = internalId / NUMBER_TRANSLATED_FRAMES;
      assert offset >= 0;
      return div + offset;
    }

    @Override
    public Frame[] allFrames() {
      return TranslatedFrame.values();
    }
  };

  private static final Map<String, SequenceMode> VALUE_OF = new HashMap<>();

  private static final SequenceMode[] VALUES = {PROTEIN, BIDIRECTIONAL, UNIDIRECTIONAL, TRANSLATED};

  /**
   * Generate array of all the possible SequenceMode singletons.
   * These are in the same ordering as ordinal().
   * @return array of all the possible SequenceMode singletons.
   */
  public static SequenceMode[] values() {
    return VALUES.clone();
  }

  static {
    for (final SequenceMode sm : values()) {
      VALUE_OF.put(sm.toString(), sm);
    }
  }

  /**
   * Get the SequenceMode singleton with the specified value (aka name).
   * @param str the name of a SequenceMode singleton.
   * @return the singleton SequenceMode
   * @throws IllegalArgumentException if str is not a valid name.
   */
  public static SequenceMode valueOf(final String str) {
    final SequenceMode res = VALUE_OF.get(str);
    if (res == null) {
      throw new IllegalArgumentException(str);
    }
    return res;
  }

  private final SequenceType mType;

  private final SequenceType mCodeType;

  private final int mOrdinal;

  private final String mValue;

  private final int mNumberFrames;

  private final int mCodeIncrement;

  /**
   * Construct singleton SequenceMode instances.
   * Is private so that only instances are the static constants.
   * @param ordinal counts each singleton.
   * @param value string rendition of name.
   * @param type the SequenceType of Residues in the sequences that have this mode.
   * @param numberFrames that can occur for one sequence with this mode.
   * @param codeIncrement increment
   */
  private SequenceMode(final int ordinal, final String value, final SequenceType type, final SequenceType codeType, final int numberFrames, final int codeIncrement) {
    mOrdinal = ordinal;
    mValue = value;
    mType = type;
    mCodeType = codeType;
    mNumberFrames = numberFrames;
    mCodeIncrement = codeIncrement;
  }

  @Override
  public String name() {
    return toString();
  }

  /**
   * Get the type of the underlying sequence.
   * @return the type of the underlying sequence.
   */
  public SequenceType type() {
    return mType;
  }

  /**
   * Get the type of the result after calling code.
   * @return the code type.
   */
  public SequenceType codeType() {
    return mCodeType;
  }

  /**
   * Get the unique integer value for the singleton instance.
   * Counts from 0 upward.
   * @return the unique integer value for the singleton instance.
   */
  @Override
  public int ordinal() {
    return mOrdinal;
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj;
  }

  @Override
  public int hashCode() {
    return ordinal() + 1;
  }

  @Override
  public String toString() {
    return mValue;
  }

  /**
   * Get the number of different frames in this mode.
   * @return number of frames in this mode
   */
  public int numberFrames() {
    return mNumberFrames;
  }

  /**
   * Get the number of underlying codes for each final code.
   * @return the code increment
   */
  public int codeIncrement() {
    return mCodeIncrement;
  }

  /**
   * Get the appropriate frame for the code.
   * For a <code>Frame</code> f the following should hold:<br>
   * <code>f == f.mode().valueOf(f.value())</code>
   * <br>
   * @param value the integer value code that uniquely specifies the frame.
   * @return the corresponding frame.
   */
  public abstract Frame frameFromCode(final int value);

  /**
   * Translate from a sequence id as used by the preread to an internal id
   * used in the SLIM index.
   * @param id the external id of the sequence.
   * @param offset the first sequence held internally.
   * @param frame the frame of the internal sequence.
   * @return the internal sequence id.
   */
  public abstract int internalId(final long id, final long offset, final Frame frame);

  /**
   * Get the frame of an internal sequence id.
   * @param internalId internal id of the sequence.
   * @return the frame of the internal sequence.
   */
  public abstract Frame frame(final int internalId);

  /**
   * Get the external sequence id corresponding to an internal sequence id.
   * @param internalId the internal sequence id.
   * @param offset the first sequence held internally.
   * @return the external sequence id.
   */
  public abstract long sequenceId(final int internalId, final long offset);

  /**
   * Get all the possible frames for this mode.
   * @return an array of all the possible frames for this mode.
   */
  public abstract Frame[] allFrames();

  /**
   * Special handling of Serialization to ensure we get singletons on deserialization.
   * @return a singleton.
   */
  Object readResolve() {
    return VALUES[ordinal()];
  }

}


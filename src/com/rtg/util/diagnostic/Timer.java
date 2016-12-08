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
package com.rtg.util.diagnostic;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

/**
 * Provide output to Diagnostic of timer information.
 * This timer can be started, stopped and reset and will log output.
 *
 * Precision is to nano-seconds and output is in fractions of a second.
 *
 * Note is taken of how many times the timer is called when it is used incrementally
 * and a mean time is output as well as the total.
 *
 * Output to the log is done in a standard form with the word "Time" as the
 * first token followed by an identifier (without any embedded spaces)
 * followed by the total time, followed by a mean time (in secs) if the count
 * is greater than 1.
 *
 * The methods can be called in the following order:
 * <code> (start, stop [toString*])* </code>
 * <code> reset </code> can be called at any time.
 */
public class Timer extends AbstractTimer implements Integrity {

  /** Accumulated time so far. */
  private long mTime = 0;

  /** Count of number of times that start has been called since last reset (or initialization). */
  private long mStartCount = 0;

  /** can hold the number of bytes read from the source*/
  private long mNumberOfBytes = 0;

  /** The time of the last start. */
  private long mStart;

  enum State {
    UNINITIALIZED, STOPPED, RUNNING
  }

  private State mState = State.UNINITIALIZED;

  /**
   * Create a new timer.
   * @param name name of timer
   */
  public Timer(final String name) {
    super(name);
  }

  @Override
  public void reset(final String name) {
    super.reset(name);
    reset();
  }

  /**
   * Reset the timer including any currently running interval and the total accumulated time.
   */
  public void reset() {
    //System.err.println("reset " + mName);
    mState = State.UNINITIALIZED;
    mStart = 0;
    mStartCount = 0;
    mTime = 0;
    mNumberOfBytes = 0;
  }

  /**
   * Start the clock ticking on a new time interval.
   */
  public void start() {
    //System.err.println("start " + mName);
    if (mState == State.RUNNING) {
      throw new IllegalStateException();
    }
    mStart = System.nanoTime();
    mState = State.RUNNING;
    ++mStartCount;
  }

  /**
   * Stop clock ticking on current interval and accumulate it into the current time.
   */
  public void stop() {
    //System.err.println("stop " + mName);
    if (mState != State.RUNNING) {
      throw new IllegalStateException(mState.toString());
    }
    mTime += System.nanoTime() - mStart;
    mStart = 0;
    mState = State.STOPPED;
  }

  /**
   * Stops the clock and accumulate time and bytes
   * @param bytes  bytes read
   */
  public void stop(final long bytes) {
    this.stop();
    mNumberOfBytes += bytes;
  }

  /**
   * Increment time when the time has been measured outside.
   * @param time the time taken in nanoseconds.
   */
  public void increment(final long time) {
    assert time >= 0;
    if (mState == State.RUNNING) {
      throw new IllegalStateException();
    }
    mTime += time;
    ++mStartCount;
    mState = State.STOPPED;
  }

  /**
   * Write the accumulated time and average time to the log.
   */
  public void log() {
    if (mState == State.RUNNING) {
      stop(); //this can occur if we have log in finally methods and an exception is thrown
    }
    Diagnostic.developerLog(toString());
  }

  /**
   * Write the accumulated time and average time to the log.
   * @param id to be appended to the name to indicate the context the timer is being used in.
   * @throws IllegalArgumentException if id contains a space.
   */
  public void log(final String id) {
    if (id.contains(" ")) {
      throw new IllegalArgumentException(id);
    }
    final StringBuilder sb = new StringBuilder();
    localToString(sb, id);
    Diagnostic.developerLog(sb.toString());
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    localToString(sb, "");
    return sb.toString();
  }

  /**
   * @param sb where to write output.
   * @param id to be appended to the name to indicate the context the timer is being used in.
   * @throws IllegalStateException
   */
  private void localToString(final StringBuilder sb, final String id) {
    if (mState == State.RUNNING) {
      throw new IllegalStateException(mState.toString());
    }
    sb.append("Timer ").append(mName);
    if (id.length() > 0) {
      sb.append("_").append(id);
    }
    if (mState == State.UNINITIALIZED) {
      sb.append(" empty");
      return;
    }
    //System.err.println(mTime);
    final double time = mTime / BILLION;
    TIME_FORMAT.format(sb, time);
    sb.append("  count ").append(mStartCount).append(" ");
    TIME_FORMAT.format(sb, time / mStartCount);
    sb.append(" bytes read ").append(mNumberOfBytes);
//    if (mStartCount > 1000) {
//      sb.append("\n^^^^ CHECK WHETHER THIS TIMER IS NECCESSARY ^^^^\n");
//    }
  }


  @Override
  public boolean integrity() {
    Exam.assertTrue(mTime >= 0);
    Exam.assertTrue(mStartCount >= 0);
    if (mState == State.RUNNING) {
      final long current = System.nanoTime();
      Exam.assertTrue(mStart <= current);
    } else if (mState == State.STOPPED) {
      Exam.assertEquals(0, mStart);
    }
    return true;
  }

  @Override
  public boolean globalIntegrity() {
    return integrity();
  }

  /**
   * Set the total time. Only to be used for testing!!
   * @param time new value for accumulated.
   */
  void setTime(final long time) {
    mTime = time;
  }
}


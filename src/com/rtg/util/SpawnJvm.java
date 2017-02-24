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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Utils for spawning a separate JVM.
 *
 */
public final class SpawnJvm {

  // private constructor so no instances can be made
  private SpawnJvm() { }

  /**
   * Spawn a process.  Maximum memory is set to 64 MiB.
   *
   * @param className class to run
   * @param args arguments to <code>main</code>
   * @return the Process object
   * @throws IOException if there is an I/O problem.
   */
  public static Process spawn(final String className, final String... args) throws IOException {
    return spawn(64 * 1025 * 1024L, className, args);
  }

  /**
   * Spawn a process with specified memory allocation.
   *
   * @param maxMem maximum amount of memory to allocate
   * @param className class to run
   * @param args arguments to <code>main</code>
   * @return the Process object
   * @throws IOException if there is an I/O problem.
   */
  public static Process spawn(final long maxMem, final String className, final String... args) throws IOException {
    return spawn(maxMem, className, false, args);
  }

  /**
   * Spawn a process with specified memory allocation.
   *
   * @param maxMem maximum amount of memory to allocate
   * @param className class to run
   * @param enableAssert assertions
   * @param args arguments to <code>main</code>
   * @return the Process object
   * @throws IOException if there is an I/O problem.
   */
  public static Process spawn(final long maxMem, final String className, final boolean enableAssert, final String... args) throws IOException {
    final String slash = System.getProperty("file.separator");
    final String javahome = System.getProperty("java.home");
    final String classpath = System.getProperty("java.class.path");
    final String librarypath = System.getProperty("java.library.path");

    final ArrayList<String> command = new ArrayList<>();
    command.add(javahome + slash + "bin" + slash + "java");
    if (enableAssert) {
      command.add("-ea");
    }
//    command.add("-server");
    command.add("-Xmx" + maxMem);
    //command.add("-Xms" + maxMem); //doesn't work on Linux ok on MacOSX back out till we understand what is going on - JC
    command.add("-cp");
    command.add(classpath);
    command.add("-Djava.library.path=" + librarypath);
    command.add(className);
    command.addAll(Arrays.asList(args));
    return new ProcessBuilder(command).start();
  }

}



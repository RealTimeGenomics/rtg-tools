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
package com.rtg.util.io;

import java.io.File;
import java.io.IOException;

import com.rtg.util.test.FileHelper;

import junit.framework.Assert;

/**
 * Class to make it easier to create temp folder to unit tests.
 * Automatically adds an appropriate name, and the close method deletes the folder
 * and all its contents
 */
public class TestDirectory extends File implements AutoCloseable {

  /**
   * Constructs with suffix using name from invoking method
   * @throws IOException it happens maybe
   */
  public TestDirectory() throws IOException {
    this(getTestName());
  }

  /**
   * @param suffix suffix for temp dir file name
   * @throws IOException it happens maybe
   */
  public TestDirectory(String suffix) throws IOException {
    this(FileUtils.createTempDir("unitTest", suffix));
  }

  /**
   * @param dir directory to use for tests
   */
  public TestDirectory(File dir) {
    super(dir.getPath());
  }

  /**
   * deletes temporary directory and all contents
   */
  @Override
  public void close() {
    if (System.getProperty("testdirectory.nocleanup") != null) {
      System.err.println("Not deleting test directory " + this.toString());
    } else {
      Assert.assertTrue(FileHelper.deleteAll(this));
    }
  }

  /**
   * for use by constructor only
   * @return class and method name of constructor invoker
   */
  private static String getTestName() {
    final StackTraceElement[] elements = new Exception().getStackTrace();
    if (elements.length > 2) {
      return elements[2].getClassName() + "." + elements[2].getMethodName();
    } else {
      return "Unknown";
    }
  }
}

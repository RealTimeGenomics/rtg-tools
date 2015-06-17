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

/**
 * Various constants applying globally to RTG.
 *
 */
public final class Constants {

  private Constants() { }

  /** Main application name */
  public static final String APPLICATION_NAME = "rtg";

  /**
   * Minimum file chunking size.  This value should be large enough to
   * accommodate a single record in files written by SLIM.
   */
  public static final long MINIMUM_FILE_CHUNK_SIZE = 1000;

  /**
   * Default maximum size that individual files can have
   * (at least the ones we write).
   */
  public static final long MAX_FILE_SIZE = 1000000000L; //1GigaByte

  /** Base for RTG email addresses */
  static final String BASE_EMAIL_ADDR = "@realtimegenomics.com";

  /** Support email address */
  public static final String SUPPORT_EMAIL_ADDR = "support" + BASE_EMAIL_ADDR;

  /** Manual crash reporting email address */
  public static final String TALKBACK_EMAIL_ADDR = "rtg-talkback" + BASE_EMAIL_ADDR;

  /** Maximum threads to allow from automatic thread assignment **/
  public static final int MAX_IO_THREADS = 4;

  /** Maximum number of files to open at once **/
  public static final int MAX_OPEN_FILES = 400; //Integer.parseInt(System.getProperty("rtg.max_open_files", "400"));

  /** Number of bytes in a KB */
  public static final double KB = 1024;

  /** Number of bytes in a MB */
  public static final double MB = 1024 * KB;

  /** Number of bytes in a GB */
  public static final double GB = 1024 * MB;
}


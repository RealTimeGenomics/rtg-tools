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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Scans class path for classes that matching criteria
 */
public class ClassPathScanner {
  private static final String CLASSPATH = System.getProperty("java.class.path");
  private static final String CLASS_EXT = ".class";
  private static final String JAR_EXT = ".jar";
  private static final char ZIP_FILE_SEPARATOR = '/'; //zip files don't use platform dependent character

  private final String mPackage;

  /**
   * Defines class acceptance criteria
   */
  public interface ClassAcceptor {
    /**
     * @param clazz class to accept or reject
     * @return true if class should be returned by {@link ClassPathScanner#getClasses(ClassAcceptor)}
     */
    boolean accept(Class<?> clazz);
  }

  /**
   * @param packagePrefix only accept classes in given package
   */
  public ClassPathScanner(String packagePrefix) {
    mPackage = packagePrefix;
  }

  /**
   * @param acceptor checks whether class should be in list
   * @return list of classes within given package that are accepted by <code>acceptor</code>
   */
  public List<Class<?>> getClasses(ClassAcceptor acceptor) {
    final List<Class<?>> testClasses = new ArrayList<>();

    try {
      for (String each : CLASSPATH.split(File.pathSeparator)) {
        final File root = new File(each);
        if (root.isDirectory()) {
          scanDirectory(testClasses, acceptor, root, root);
        } else if (root.getName().endsWith(JAR_EXT)) {
          scanZipFile(testClasses, acceptor, root);
        }
      }
      testClasses.sort(Comparator.comparing(Class::getName));
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
    return testClasses;
  }

  private static String getClassName(String fileName, String classPathRoot, char separatorChar) {
    String className = fileName.substring(classPathRoot.length());
    if (className.charAt(0) == separatorChar) {
      className = className.substring(1);
    }
    return className.substring(0, className.length() - CLASS_EXT.length()).replace(separatorChar, '.');
  }

  private void scanDirectory(List<Class<?>> classes, ClassAcceptor acceptor, File root, File cur) {
    final File[] files = cur.listFiles();
    if (files != null) {
      for (File each : files) {
        if (each.isDirectory()) {
          scanDirectory(classes, acceptor, root, each);
        } else {
          if (each.getName().endsWith(CLASS_EXT)) {
            final String className = getClassName(each.getPath(), root.getPath(), File.separatorChar);
            checkClass(classes, acceptor, className);
          }
        }
      }
    }
  }

  private void checkClass(List<Class<?>> classes, ClassAcceptor acceptor, String className) {
    if (className.startsWith(mPackage)) {
      try {
        final Class<?> clazz = Class.forName(className);
        if (acceptor.accept(clazz)) {
          classes.add(clazz);
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private void scanZipFile(List<Class<?>> classes, ClassAcceptor acceptor, File zipname) {
    try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipname))) {
      ZipEntry each;
      while ((each = zin.getNextEntry()) != null) {
        if (each.getName().endsWith(CLASS_EXT)) {
          final String className = getClassName(each.getName(), "", ZIP_FILE_SEPARATOR);
          checkClass(classes, acceptor, className);
        }
        zin.closeEntry();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

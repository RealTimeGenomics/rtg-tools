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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.StringUtils;
import com.rtg.util.gzip.GzipUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogFile;
import com.rtg.util.io.LogSimple;
import com.rtg.util.io.LogStream;

/**
 * Utility class for controlling the dispatch of diagnostic warnings, errors,
 * and progress information in SLIM.  Anyone implementing <code>DiagnosticListener</code>
 * can register to receive diagnostic events, and anyone can generate such events
 * by calling the utility methods in this class.
 *
 * <p>A facility is also provided for logging of text messages to a stream. By
 * default such output is sent to <code>System.err</code>, but any other
 * <code>PrintStream</code> can be supplied.
 *
 */
public final class Diagnostic {

  /** flush interval used for flushing logs */
  private static final int FLUSH_INTERVAL = 1000;

  private Diagnostic() { }

  /** Maintains all current listeners. */
  private static final HashSet<DiagnosticListener> LISTENERS = new HashSet<>();

  /** Current stream for logging, if any. */
  private static LogStream sLogStream = new LogSimple(System.err);

  /** True if the current log is closed. */
  private static boolean sLogClosed = false;

  private static LogStream sProgressStream = null;

  private static boolean sProgressClosed = false;

  private static String sLastProgress = "";

  /** Tracks if we have reported logging redirection message. */
  private static boolean sLogRedirect = false;

  /** tracks when the last flush happened and attempts to flush every minute **/
  private static long sLastFlushTime = 0;

  /**
   * Remove all listeners.
   */
  public static void clearListeners() {
    LISTENERS.clear();
  }

  /**
   * Add the given listener to the those notified whenever a diagnostic occurs.
   * Requests to add null are ignored.  Adding the same listener multiple times
   * is the same as adding it once.
   *
   * @param listener listener to add
   */
  public static void addListener(final DiagnosticListener listener) {
    if (listener != null) {
      LISTENERS.add(listener);
    }
  }

  /**
   * Remove a listener from those notified whenever a diagnostic occurs. Requests
   * to remove a listener not currently registered are ignored.
   *
   * @param listener listener to remove
   */
  public static void removeListener(final DiagnosticListener listener) {
    if (listener != null) {
      listener.close();
    }
    LISTENERS.remove(listener);
  }

  /**
   * Pass the given diagnostic event to all currently registered listeners.
   * Listeners should not modify the event.  Null events are not passed.
   *
   * @param event the event
   */
  public static void notifyAll(final DiagnosticEvent<?> event) {
    if (event != null) {
      for (final DiagnosticListener listener : LISTENERS) {
        listener.handleDiagnosticEvent(event);
      }
    }
  }

  /**
   * Convenience method to report a warning.  If the <code>type</code> is
   * <code>null</code> or if the parameter count does not match the expected
   * number of parameters then the notification is ignored.
   *
   * @param type type of warning
   * @param params parameters of warning
   */
  public static void warning(final WarningType type, final String... params) {
    if (type != null && type.getNumberOfParameters() == params.length) {
      final WarningEvent event = new WarningEvent(type, params);
      userLog(event.getMessage());
      notifyAll(event);
    }
  }

  /**
   * Convenience method to send a message to the user. This raises a warning event
   * of type <code>WarningType.INFO_WARNING</code>.
   * @param message message to print
   */
  public static void warning(final String message) {
    warning(WarningType.INFO_WARNING, message);
  }

  /**
   * Convenience method to report an error to the log only.  If the <code>type</code> is
   * <code>null</code> or if the parameter count does not match the expected
   * number of parameters then the notification is ignored.
   *
   * @param type type of error
   * @param params parameters of error
   */
  public static void errorLogOnly(final ErrorType type, final String... params) {
    if (type != null && type.getNumberOfParameters() == params.length) {
      final ErrorEvent event = new ErrorEvent(type, params);
      userLog(event.getMessage());
    }
  }

  /**
   * Convenience method to report an error.  If the <code>type</code> is
   * <code>null</code> or if the parameter count does not match the expected
   * number of parameters then the notification is ignored.
   *
   * @param type type of error
   * @param params parameters of error
   */
  public static void errorNoLog(final ErrorType type, final String... params) {
    if (type != null && type.getNumberOfParameters() == params.length) {
      final ErrorEvent event = new ErrorEvent(type, params);
      //System.err.println(event.getMessage());
      notifyAll(event);
    }
  }

  /**
   * Convenience method to report an error.  If the <code>type</code> is
   * <code>null</code> or if the parameter count does not match the expected
   * number of parameters then the notification is ignored.  Any error
   * reported here, is also sent to the log.
   *
   * @param type type of error
   * @param params parameters of error
   */
  public static void error(final ErrorType type, final String... params) {
    if (type != null && type.getNumberOfParameters() == params.length) {
      final ErrorEvent event = new ErrorEvent(type, params);
      userLog(event.getMessage());
      notifyAll(event);
    }
  }

  /**
   * Convenience method to report an error. Uses <code>INFO_ERROR</code> to send
   * error event.
   * @param message the message for the error.
   */
  public static void error(String message) {
    error(ErrorType.INFO_ERROR, message);
  }

  /**
   * Convenience method to report user information.  If the <code>type</code> is
   * <code>null</code> or if the parameter count does not match the expected
   * number of parameters then the notification is ignored.
   *
   * @param type type of information
   * @param params parameters of user information
   */
  public static void info(final InformationType type, final String... params) {
    info(type, false, params);
  }

  /**
   * Convenience method to report user information.  If the <code>type</code> is
   * <code>null</code> or if the parameter count does not match the expected
   * number of parameters then the notification is ignored.
   *
   * @param type type of information
   * @param outputToProgress true if you want to output the message to the progress stream
   * @param params parameters of user information
   */
  public static void info(final InformationType type, boolean outputToProgress, final String... params) {
    if (type != null && type.getNumberOfParameters() == params.length) {
      final InformationEvent event = new InformationEvent(type, params);
      userLog(event.getMessage());
      if (outputToProgress) {
        Diagnostic.progress(event.getMessage());
      }
      notifyAll(event);
    }
  }

  /**
   * Convenience method to report information to user. Uses <code>INFO_USER</code> to send
   * information event.
   * @param message the message for the user.
   */
  public static void info(String message) {
    info(InformationType.INFO_USER, message);
  }

  /**
   * Set the stream to which logging messages are sent to null.  This is permissible,
   * but unadvisable, as setting it to null causes all
   * logging information to be discarded.   By default to logging stream
   * is System.err.
   * However, this is handy for testing.
   */
  public static void setLogStream() {
    setLogStream((LogStream) null);
  }

  /**
   * Set the stream to which logging messages are sent.  It is permissible,
   * but unadvisable to set this to null, as setting it to null causes all
   * logging information to be discarded.   By default to logging stream
   * is System.err.
   *
   * @param logStream stream to use for logging
   * @return the log stream created
   */
  public static LogStream setLogStream(final PrintStream logStream) {
    final LogStream ret = new LogSimple(logStream);
    setLogStream(ret);
    return ret;
  }

  /**
   * Set the stream to which logging messages are sent.  It is permissible,
   * but unadvisable to set this to null, as setting it to null causes all
   * logging information to be discarded.
   *
   * @param logStream stream to use for logging
   */
  public static synchronized void setLogStream(final LogStream logStream) {
    sLogRedirect = false;
    if (sLogStream == logStream) {
      return;
    }
    if (sLogStream != null && logStream != null && logStream.stream() == sLogStream.stream()) {
      return;
    }
    if (sLogStream != null) {
      closeLog();
    }
    sLogStream = logStream;
    sLogClosed = false;
    sProgressClosed = false;
  }

  /**
   * @return the stream currently used for log messages.
   */
  static synchronized PrintStream getLogStream() {
    if ((sLogStream == null) || sLogClosed) {
      return null;
    }
    return sLogStream.stream();
  }

  /**
   * @return the stream currently used for progress messages.
   */
  static synchronized PrintStream getProgressStream() {
    if (sProgressClosed || sProgressStream == null && (sLogStream == null || sLogStream.file() == null)) {
      return null;
    }
    if (sProgressStream == null) {
      final File progressFile = new File(sLogStream.file().getParentFile(), FileUtils.PROGRESS_SUFFIX);
      sProgressStream = new LogFile(progressFile);

    }
    return sProgressStream.stream();
  }


  /**
   * Switch the log to a (usually) different output file.
   * @param log the name of the file where the log is to be written.
   * @return the log (so it can be closed).
   */
  public static synchronized LogStream switchLog(final String log) {
    final File newLog = new File(log);
    return switchLog(newLog);
  }

  /**
   * Switch the log to a (usually) different output file.
   * @param newLog the file where the log is to be written.
   * @return the log (so it can be closed).
   */
  public static synchronized LogStream switchLog(final File newLog) {
    if (sLogStream != null) {
      userLog("Switching logfile to:" + newLog.getAbsolutePath());
      final File file = sLogStream.file();
      if (newLog.equals(file)) {
        return sLogStream;
      }
      //System.err.println("Will delete: " + file.getPath());
      sLogStream.removeLog();
    }
    //System.err.println("Switching logfile to:" + log);
    if (!newLog.getParentFile().exists()) {
      if (!newLog.getParentFile().mkdirs()) {
        throw new RuntimeException("Unable to create directory for log file.");
      }
    }
    sLogStream = new LogFile(newLog);
    sLogClosed = false;
    logEnvironment();
    return sLogStream;
  }

  /**
   * Delete the file currently used for logging.  This should only be called
   * when you are certain the current logging file is not required.  After
   * calling this, the current logging stream is set to <code>null</code>.
   */
  public static synchronized void deleteLog() {
    if (sLogStream != null) {
      sLogStream.removeLog();
      setLogStream();
    }
  }

  /**
   * Closes the current log stream, should be used at the end of a main to ensure
   * any logging files are closed if it is difficult to ensure a created log stream
   * is closed.
   */
  public static synchronized void closeLog() {
    if (sLogStream != null) {
      sLogStream.close();
      sLogClosed = true;
    }
    if (sProgressStream != null) {
      sProgressStream.close();
      sProgressClosed = true;
      sProgressStream = null;
    }
  }


  /**
   * Write a message to the log stream.  If writing to the log stream fails
   * and the log stream differs from <code>System.err</code>, then an attempt
   * is made to redirect the message to <code>System.err</code>.
   *
   * @param message to write
   */
  public static synchronized void userLog(final String message) {
    userLog(message, "");
  }


  /**
   * Write a message to the log stream.  If writing to the log stream fails
   * and the log stream differs from <code>System.err</code>, then an attempt
   * is made to redirect the message to <code>System.err</code>.
   *
   * @param message to write
   * @param prefix a prefix to include before the message
   */
  private static synchronized void userLog(final String message, final String prefix) {
    final PrintStream log = getLogStream();
    if (log != null) {
      log.println(now() + prefix + message);
      final long currentTimeMillis = System.currentTimeMillis();
      if (currentTimeMillis - sLastFlushTime > FLUSH_INTERVAL) {
        log.flush();
        sLastFlushTime = currentTimeMillis;
      }
      if (log.checkError()) {
        // The call to checkError forces a flush and returns true if the stream
        // is in an unusable state.  This should not happen in ordinary usage,
        // but could happen if for example logging is going to a file and the
        // disk runs out of space.  Therefore, if the current logging stream is
        // not System.err, we make an effort to redirect the log message to
        // that stream.
        if (!log.equals(System.err)) {
          if (!sLogRedirect) {
            sLogRedirect = true;
            System.err.println("Logging problem: redirecting logging to System.err.");
            setLogStream(System.err);
          }
          System.err.println(now() + message);
          System.err.flush();
        }
      }
    }
  }

  /**
   * Write a throwable to the log stream. If writing to the log stream fails
   * and the log stream differs from <code>System.err</code>, then an attempt
   * is made to redirect the message to <code>System.err</code>.
   *
   * @param t throwable to write
   */
  public static synchronized void userLog(final Throwable t) {
    final PrintStream log = getLogStream();
    if (log != null) {
      t.printStackTrace(log);
      log.flush();
      if (log.checkError()) {
        // The call to checkError forces a flush and returns true if the stream
        // is in an unusable state.  This should not happen in ordinary usage,
        // but could happen if for example logging is going to a file and the
        // disk runs out of space.  Therefore, if the current logging stream is
        // not System.err, we make an effort to redirect the log message to
        // that stream.
        if (!log.equals(System.err)) {
          if (!sLogRedirect) {
            sLogRedirect = true;
            System.err.println(now() + "Logging problem: redirecting logging to System.err.");
          }
          t.printStackTrace(System.err);
          System.err.flush();
        }
      }
    }
  }

  private static void logVersion() {
    userLog("RTG version = " + Environment.getVersion());
  }

  /**
   * Write a bunch of environmental information to the log stream, in the form
   * of <code>(key,value)</code> pairs.
   */
  public static synchronized void logEnvironment() {
    userLog("serial = " + License.getSerialNumber());
    userLog("email = " + License.getPersonEmail());
    logVersion();
    final SortedMap<String, String> env = new TreeMap<>(Environment.getEnvironmentMap());
    for (final Map.Entry<String, String> e : env.entrySet()) {
      userLog(e.getKey() + " = " + e.getValue());
    }
    userLog("gzipfix.enabled = " + GzipUtils.getOverrideGzip());
  }

  /**
   * Get the current date and time as a string of the form <code>YYYY-MM-DD hh:mm:ss</code>.
   *
   * @return date string
   */
  public static String now() {
    final StringBuilder sb = new StringBuilder();
    final Calendar cal = new GregorianCalendar();
    sb.append(cal.get(Calendar.YEAR)).append('-');
    final int month = 1 + cal.get(Calendar.MONTH);
    if (month < 10) {
      sb.append('0');
    }
    sb.append(month).append('-');
    final int date = cal.get(Calendar.DATE);
    if (date < 10) {
      sb.append('0');
    }
    sb.append(date).append(' ');
    final int hour = cal.get(Calendar.HOUR_OF_DAY);
    if (hour < 10) {
      sb.append('0');
    }
    sb.append(hour).append(':');
    final int min = cal.get(Calendar.MINUTE);
    if (min < 10) {
      sb.append('0');
    }
    sb.append(min).append(':');
    final int sec = cal.get(Calendar.SECOND);
    if (sec < 10) {
      sb.append('0');
    }
    sb.append(sec).append(' ');
    return sb.toString();
  }

  static File getLogFile() {
    return sLogStream == null ? null : sLogStream.file();
  }

  /**
   * Write a message to the log stream for developers only.
   * If writing to the log stream fails and the log stream differs from
   * <code>System.err</code>, then an attempt is made to redirect the message
   * to <code>System.err</code>.
   *
   * @param message to write
   */
  public static void developerLog(final String message) {
    if (License.isDeveloper()) {
      userLog(message, "\t\t "); // prefix developer only log messages so we can visually separate developers versus customers
    }
  }

  /**
   * Write a throwable to the log stream for developers only.
   * If writing to the log stream fails and the log stream differs from
   * <code>System.err</code>, then an attempt is made to redirect the message
   * to <code>System.err</code>.
   *
   * @param t throwable to write
   */
  public static void developerLog(final Throwable t) {
    if (License.isDeveloper()) {
      userLog(t);
    }
  }

  /**
   * Write a message to the progress stream if a log file exists.
   * @param message to write
   */
  public static synchronized void progress(final String message) {
    sLastProgress = message;
    final PrintStream prog = getProgressStream();
    if (prog != null) {
      prog.println(now() + message);
      prog.flush();
      if (prog.checkError()) {
        // The call to checkError forces a flush and returns true if the stream
        // is in an unusable state.  This should not happen in ordinary usage,
        // but could happen if for example logging is going to a file and the
        // disk runs out of space.
        sProgressStream = null;
      }
    }
  }

  /**
   * Gets the last progress message.
   *
   * @return the last progress message.
   */
  public static synchronized String lastProgress() {
    return sLastProgress;
  }

  private static final String OOM_ERROR_MESSAGE = StringUtils.LS + new ErrorEvent(ErrorType.NOT_ENOUGH_MEMORY).getMessage() + StringUtils.LS;
  private static final byte[] OOM_ERROR_MESSAGE_BYTES = OOM_ERROR_MESSAGE.getBytes();
  private static final OutputStream OOM_ERROR_STREAM = FileUtils.getStderrAsOutputStream();

  /**
   * Prints a message about being out of memory, without allocating additional memory.
   */
  public static void oomMessage() {
    try {
      OOM_ERROR_STREAM.write(OOM_ERROR_MESSAGE_BYTES);
      OOM_ERROR_STREAM.flush();
    } catch (final IOException e) {
      // can't do anything else here without allocating more memory
    }
  }

}


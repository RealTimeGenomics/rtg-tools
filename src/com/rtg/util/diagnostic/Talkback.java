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

import static com.rtg.util.StringUtils.LS;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rtg.util.Constants;
import com.rtg.util.Environment;
import com.rtg.util.License;
import com.rtg.util.Utils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.io.FileUtils;

/**
 * User-controllable facility for sending stack traces and logs to us.
 *
 */
public final class Talkback {

  //private constructor as this is a utility class
  private Talkback() {
  }

  private static String sTalkbackURL;
  private static boolean sTalkback;
  static {
    final String talkbackValue = System.getProperty("talkback", Boolean.toString(!License.isDeveloper()));
    sTalkback = Boolean.parseBoolean(talkbackValue);
    sTalkbackURL = System.getProperty("talkback.url", "https://api.realtimegenomics.com/talkback/submit.php");
  }

  /* For testing */
  static void setTalkback(final boolean enable) {
    sTalkback = enable;
  }
  static void setTalkbackURL(final String u) {
    sTalkbackURL = u;
  }

  private static String sModuleName = null;

  private static boolean doPost(final String url, final String contentType, final String body, final int timeout) {
    try {
      final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setConnectTimeout(timeout);
      conn.setReadTimeout(timeout);
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);

      final byte[] data = body.getBytes();
      conn.setRequestProperty("Content-Type", contentType);
      try (OutputStream postStream = conn.getOutputStream()) {
        postStream.write(data);
      }
      return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException("Bad Url", e);
    } catch (final IOException e) {
      return false;
    }
  }

  /**
   * A talkback method for wrapper scripts to call. E.g. on JVM crash, or for talkback testing. Assume that first argument is the log file location
   * if not the empty string and all remaining arguments
   * are sent as command line arguments.
   * @param args line arguments
   * @return false if the talkback was not sent.
   */
  protected static boolean commandLineTalkback(String[] args) {
    try {
      final File logFile;
      if (args.length > 0 && !"".equals(args[0])) {
        logFile = new File(args[0]);
      } else {
        logFile = null;
      }
      final String filename = (logFile != null && logFile.exists()) ? args[0] : "";
      final boolean crashReport = !"".equals(filename);
      final int argStart = crashReport ? 1 : 0;

      final String subject = crashReport ? "JVM Crash" : "Talkback";
      final String commandLine = args.length > argStart ? Arrays.toString(Arrays.copyOfRange(args, argStart, args.length)) : null;
      final String logContents;
      if (crashReport) {
        logContents = getEnvironment() + FileUtils.fileToString(logFile);
      } else {
        logContents = getEnvironment();
      }
      return postTalkback(subject, sModuleName, commandLine, null, logContents, filename, crashReport);
    } catch (final IOException e) {
      System.err.println("An error occurred sending the talkback.");
      return false;
    }
  }


  /**
   * Post a talk back message, supplying the parameters automatically
   * from an exception, the key and the environment.  The timeout is set to 5s.
   *
   * @param t the exception
   * @param prompt whether to print on System.err the progress
   * @return true if posted ok, false if an error occurred on the talk back page or a timeout
   */
  public static boolean postTalkback(final Throwable t, final boolean prompt) {
    try {
      final File f = Diagnostic.getLogFile();
      final String filename = f == null ? "" : f.getPath();

      final String stacktrace = Utils.getStackTrace(t);
      if (sModuleName == null || inTest(stacktrace)) {
        return true;
      }
      final String subject = t.getMessage();
      final String commandLine = Arrays.toString(CommandLine.getCommandArgs());
      final String logContents = getLogContents();

      return postTalkback(subject, sModuleName, commandLine, stacktrace, logContents, filename, prompt);
    } catch (final IOException e) {
      if (prompt) {
        System.err.println("An error occurred sending the talkback.");
      }
      return false;
    }
  }

  private static void appendPostData(StringBuilder postData, String param, String value) {
    if (value != null) {
      if (postData.length() > 0) {
        postData.append('&');
      }
      postData.append(param).append('=').append(urlEscape(value));
    }
  }

  private static boolean postTalkback(String subject, String moduleName, String commandLine, String stacktrace, String logFull, String filename, boolean prompt) {
    final boolean hasLogfile = !"".equals(filename);
    if (!sTalkback) {
      if (prompt) {
        if (hasLogfile) {
          System.err.println("Please send the log file ('" + filename + "') to Real Time Genomics at " + Constants.TALKBACK_EMAIL_ADDR);
        }
      }
      return true;
    }

    final String heretrace = Utils.getStackTrace(new Throwable());
    if (inTest(heretrace)) {
      return true;
    }

    if (prompt) {
      String message = "Sending talkback to Real Time Genomics";
      if (hasLogfile) {
        message += " (log '" + filename + "')";
      }
      System.err.println(message + "...");
    }

    String logContents = logFull;
    if (logContents.length() > 41e3) {
      logContents = logContents.substring(0, 10000) + "...\n\n(middle of log excluded due to being oversize)\n\n..." + logContents.substring(logContents.length() - 30000);
      if (prompt) {
        System.err.println("The log file is oversized, sending truncated log.  Please send the full log file ('" + filename + "') to Real Time Genomics at " + Constants.TALKBACK_EMAIL_ADDR);
      }
    }
    final StringBuilder postData = new StringBuilder();
    appendPostData(postData, "module", moduleName);
    appendPostData(postData, "commandline", commandLine);
    appendPostData(postData, "stacktrace", stacktrace);
    appendPostData(postData, "log", logContents);

    final boolean status = postTalkback(subject, 5000, postData.toString());
    if (prompt) {
      if (status) {
        System.err.println("Talkback successfully sent.");
      } else {
        System.err.println("An error occurred sending the talkback.");
        if (hasLogfile) {
          System.err.println("Please send the log file ('" + filename + "') to Real Time Genomics at " + Constants.TALKBACK_EMAIL_ADDR);
        }
      }
      final String current = Environment.getProductVersion();
      final String latest = Environment.getLatestReleaseVersion();
      if (latest != null && !latest.equals(current)) {
        System.err.println();
        System.err.println("NOTE: You are using " + Environment.getProductName() + " version " + current + " but the latest release is " + latest + ". You may want to upgrade.");
        System.err.println();
      } else {
        System.err.println();
        System.err.println("NOTE: You are using " + Environment.getProductName() + " version " + current + ". Please check whether a newer version is available, since if this is a bug it may already be fixed.");
        System.err.println();
      }
    }
    return status;
  }

  /**
   * Post a talk back message with prompting, supplying the parameters automatically
   * from an exception, the key and the environment.  The timeout is set to 5s.
   *
   * @param t the exception
   * @return true if posted ok, false if an error occurred on the talk back page or a timeout
   */
  public static boolean postTalkback(final Throwable t) {
    return postTalkback(t, true);
  }

  /**
   * Post a Talkback message, supplying all the details manually.
   *
   * @param subject subject of the email
   * @param timeout timeout in ms for read and connect (max elapsed time is double timeout)
   * @param postData data to send the the post part, must be already formatted
   * @return true if posted ok, false if an error occurred on the talk back page or a timeout
   */
  public static boolean postTalkback(final String subject, final int timeout, final String postData) {
    final String user =  System.getProperty("user.name");
    final String machine = Environment.getHostName();
    final String expiry  = License.getExpirationDate();
    final String fullsubject = License.getOrganisation() + "/" + user + ", " + subject + ", " + Environment.getVersion();
    final String url = sTalkbackURL
        + "?user=" + urlEscape(user)
        + "&machine=" + urlEscape(machine)
        + "&expiry=" + urlEscape(expiry)
        + "&subject=" + urlEscape(fullsubject)
        + "&d=" + (License.isDeveloper() ? "1" : "0");
    return doPost(url, "application/x-www-form-urlencoded", postData, timeout);
  }

  /**
   * Returns a string listing enviroment properties for submitting via talkback.
   * @return the environment string
   */
  public static String getEnvironment() {
    final StringBuilder sb = new StringBuilder();
    sb.append("serial = ").append(License.getSerialNumber()).append(LS);
    sb.append("email = ").append(License.getPersonEmail()).append(LS);
    sb.append("RTG version = ").append(Environment.getVersion()).append(LS);
    final SortedMap<String, String> env = new TreeMap<>(Environment.getEnvironmentMap());
    for (final Map.Entry<String, String> e : env.entrySet()) {
      sb.append(e.getKey()).append(" = ").append(e.getValue()).append(LS);
    }
    return sb.toString();
  }

  private static String getLogContents() throws IOException {
    final File f = Diagnostic.getLogFile();
    if (f == null) {
      return "<no log stream set>";
    }
    return FileUtils.fileToString(f);
  }

  private static final Pattern URL_ESCAPE_PATTERN = Pattern.compile("[^A-Za-z0-9\\-_.]");

  private static String urlEscape(final String s) {
    if (s == null) {
      return "NULL";
    }
    final Matcher m = URL_ESCAPE_PATTERN.matcher(s);
    final StringBuffer sb = new StringBuffer();
    while (m.find()) {
      final char c = m.group(0).charAt(0);
      m.appendReplacement(sb, "%" + hex(c));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  private static final String HEX_DIGITS = "0123456789ABCDEF";
  private static String hex(final char c) {
    return String.valueOf(HEX_DIGITS.charAt((c >> 4) & 15)) + String.valueOf(HEX_DIGITS.charAt(c & 15));
  }

  private static final Pattern[] NOT_IN_TEST_PATTERNS = new Pattern[]
    {Pattern.compile("\n\\s+at com.rtg.util.diagnostic.TalkbackTest")};
  private static final Pattern[] IN_TEST_PATTERNS = new Pattern[]
    {Pattern.compile("\n\\s+at junit\\."),
    Pattern.compile("\n\\s+at com.rtg.util.SimpleThreadPoolTest\\."),
    Pattern.compile("Test\\.java:\\d+\\)\r?\n")};

  private static boolean inTest(final String stacktrace) {
    for (final Pattern p : NOT_IN_TEST_PATTERNS) {
      final Matcher m = p.matcher(stacktrace);
      if (m.find()) {
        return false;
      }
    }
    for (final Pattern p : IN_TEST_PATTERNS) {
      final Matcher m = p.matcher(stacktrace);
      if (m.find()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the SLIM module name.
   *
   * @param name the module name
   */
  public static void setModuleName(final String name) {
    sModuleName = name;
  }
}

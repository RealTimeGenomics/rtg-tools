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
package com.rtg.graph;

import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.REPORTING;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;

/**
 */
public class RocPlotCli extends AbstractCli {
  private static final String HTML_FLAG = "Xhtml";
  private static final String APPLET_FLAG = "Xapplet";
  private static final String HIDE_SIDEPANE_FLAG = "hide-sidepane";
  private static final String PNG_FLAG = "png";
  static final String TITLE_FLAG = "title";
  static final String SCORES_FLAG = "scores";
  static final String LINE_WIDTH_FLAG = "line-width";
  static final String CURVE_FLAG = "curve";


  static final String PNG_EXTENSION = ".png";

  private static final String HTML_PREAMBLE = ""
      + "<html>\n"
      + "  <head>\n"
      + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"global.css\"/>\n"
      + "  </head>\n"
      + "  <body>\n"
      + "  <div>";
  private static final String HTML_POSTAMBLE = ""
      + "  </div>\n"
      + "  </body>\n"
      + "</html>";


  private static class FlagValidator implements Validator {
    @Override
    public boolean isValid(CFlags flags) {
      if (!(flags.getAnonymousValues(0).size() > 0 || flags.isSet(CURVE_FLAG))) {
        flags.error("Must supply at least 1 ROC file");
        return false;
      }
      if (flags.isSet(CURVE_FLAG)) {
        for (Pair<File, String> filepair : parseNamedFileStrings(flags.getValues(CURVE_FLAG))) {
          if (!filepair.getA().exists()) {
            flags.error("File: " + filepair.getA() + " does not exist");
            return false;
          }
        }
      }
      for (Object o : flags.getAnonymousValues(0)) {
        final File f = (File) o;
        if (!f.exists()) {
          flags.error("File: " + f + " does not exist");
          return false;
        }
      }
      if (flags.isSet(PNG_FLAG)) {
        final File pngFile = getPngFilename((File) flags.getValue(PNG_FLAG));
        if (pngFile.isDirectory()) {
          flags.error("Path: " + pngFile.getPath() + " is a directory");
          return false;
        } else if (pngFile.exists()) {
          flags.error("File: "  + pngFile.getPath() + " already exists");
          return false;
        }
      }
      if (!CommonFlags.validateFlagBetweenValues(flags, LINE_WIDTH_FLAG, RocPlot.LINE_WIDTH_MIN, RocPlot.LINE_WIDTH_MAX)) {
        return false;
      }
      return true;
    }
  }
  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  static void initFlags(CFlags flags) {
    CommonFlagCategories.setCategories(flags);
    flags.setDescription("Plot ROC curves from vcfeval ROC data files, either to an image, or an interactive GUI.");
    flags.registerExtendedHelp();
    flags.registerOptional(HTML_FLAG, "if set, output text for an HTML page that would display the ROC using an applet").setCategory(REPORTING);
    flags.registerOptional(APPLET_FLAG, "if set, output text for an HTML applet tag").setCategory(REPORTING);
    flags.registerOptional('t', TITLE_FLAG, String.class, "STRING", "title for the plot").setCategory(REPORTING);
    flags.registerOptional(SCORES_FLAG, "if set, show scores on the plot").setCategory(REPORTING);
    flags.registerOptional(HIDE_SIDEPANE_FLAG, "if set, hide the sidepane from the GUI on startup").setCategory(REPORTING);
    flags.registerOptional(LINE_WIDTH_FLAG, Integer.class , "INT", "sets the plot line width", 2).setCategory(REPORTING);
    flags.registerOptional(PNG_FLAG, File.class , "FILE", "if set, output a PNG image to the given file").setCategory(INPUT_OUTPUT);
    final Flag curveFlag = flags.registerOptional(CURVE_FLAG, String.class, "STRING", "ROC data file with title optionally specified (path[=title])").setCategory(INPUT_OUTPUT);
    curveFlag.setMinCount(0);
    curveFlag.setMaxCount(Integer.MAX_VALUE);
    final Flag fileFlag = flags.registerRequired(File.class, "FILE", "ROC data file").setCategory(INPUT_OUTPUT);
    fileFlag.setMinCount(0);
    fileFlag.setMaxCount(Integer.MAX_VALUE);
    flags.addRequiredSet(fileFlag);
    flags.addRequiredSet(curveFlag);
    flags.setValidator(new FlagValidator());
  }

  private static boolean isReallyHeadless() {
    if (GraphicsEnvironment.isHeadless()) {
      return true;
    }
    try {
      final GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
      return screenDevices == null || screenDevices.length == 0;
    } catch (InternalError | UnsatisfiedLinkError e) {
      Diagnostic.error(e.getMessage());
      return true;
    } catch (HeadlessException e) {
      return true;
    }
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final ArrayList<File> fileList = new ArrayList<>();
    final ArrayList<String> nameList = new ArrayList<>();
    extractNamedFiles(mFlags.getFlag(CURVE_FLAG), mFlags.getAnonymousFlag(0), fileList, nameList);

    final BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(out));
    try {
      if (mFlags.isSet(HTML_FLAG) || mFlags.isSet(APPLET_FLAG)) {
        rocApplet(fileList, nameList, outWriter, mFlags.isSet(HTML_FLAG), mFlags.isSet(SCORES_FLAG), mFlags.getValue(TITLE_FLAG), mFlags.isSet(HIDE_SIDEPANE_FLAG), (Integer) mFlags.getValue(LINE_WIDTH_FLAG));
      } else if (mFlags.isSet(PNG_FLAG))    { // Just output a PNG image
        System.setProperty("java.awt.headless", "true");
        RocPlotPng.rocPngImage(fileList, nameList, (String) mFlags.getValue(TITLE_FLAG), mFlags.isSet(SCORES_FLAG), (Integer) mFlags.getValue(LINE_WIDTH_FLAG), getPngFilename((File) mFlags.getValue(PNG_FLAG)));
      } else {   //Create and set up as a stand alone app.
        if (isReallyHeadless()) {
          Diagnostic.error("No graphics environment is available to open the rocplot GUI");
          return 1;
        }
        UIManager.put("Slider.paintValue", Boolean.FALSE); // Make GTK theme more bearable, if used
        final RocPlot rp = new RocPlot();
        rp.rocStandalone(fileList, nameList, (String) mFlags.getValue(TITLE_FLAG), mFlags.isSet(SCORES_FLAG), mFlags.isSet(HIDE_SIDEPANE_FLAG), (Integer) mFlags.getValue(LINE_WIDTH_FLAG));
      }
    } catch (InvocationTargetException e) {
      //should only be possible to have runtime
      Diagnostic.developerLog(e);
      final Throwable target = e.getTargetException();
      if (target instanceof RuntimeException) {
        throw (RuntimeException) target;
      } else {
        //shouldn't be possible, investigate
        throw new RuntimeException(target);
      }
    } catch (InterruptedException e) {
      Diagnostic.warning("Interrupted, aborting...");
      return 1;
    } finally {
      outWriter.flush();
    }
    return 0;
  }

  private static void rocApplet(ArrayList<File> fileList, ArrayList<String> nameList, BufferedWriter outWriter, boolean html, boolean scores, Object title, boolean hideSidepane, Integer lineWidthSetting) throws IOException {
    if (html) {
      outWriter.write(HTML_PREAMBLE);
      outWriter.newLine();
    }
    outWriter.write("    <applet width=\"100%\" height=\"100%\" code=\"com.rtg.graph.RocApplet\" archive=\"/ROC.jar\">");
    outWriter.newLine();
    if (title != null) {
      outWriter.write("      <param name=\"" + RocApplet.PARAM_TITLE + "\" value=\"" + title + "\">");
      outWriter.newLine();
    }
    if (scores) {
      outWriter.write("      <param name=\"" + RocApplet.PARAM_SCORES + "\" value=\"true\">");
      outWriter.newLine();
    }
    if (hideSidepane) {
      outWriter.write("      <param name=\"" + RocApplet.PARAM_SIDEPANE + "\" value=\"true\">");
      outWriter.newLine();
    }
    if (lineWidthSetting != null) {
      outWriter.write("      <param name=\"" + RocApplet.PARAM_LINEWIDTH + "\" value=\"" + lineWidthSetting + "\">");
      outWriter.newLine();
    }
    for (int i = 0; i < fileList.size(); i++) {
      outWriter.write("      <param name=\"" + RocApplet.PARAM_DATA + (i + 1) + "\" value=\"" + fileList.get(i) + "\">");
      outWriter.newLine();
      outWriter.write("      <param name=\"" + RocApplet.PARAM_NAME + (i + 1) + "\" value=\"" + nameList.get(i) + "\">");
      outWriter.newLine();
    }
    outWriter.write("    </applet>");
    outWriter.newLine();
    if (html) {
      outWriter.write(HTML_POSTAMBLE);
      outWriter.newLine();
    }
  }

  /**
   * Extracts the directory name of a file. This is sometimes used as a default name when displaying tracks etc
   * @param file the file
   * @return the directory name
   */
  static String defaultNameFromFile(File file) {
    return file.getParentFile() != null ? file.getParentFile().getName() : file.getPath();
  }

  static List<Pair<File, String>> parseNamedFileStrings(List<Object> curveStrings) {
    final ArrayList<Pair<File, String>> ret = new ArrayList<>();
    for (final Object o : curveStrings) {
      final String curveString = (String) o;
      final Pair<File, String> namedFile = parseNamedFileString(curveString);
      ret.add(namedFile);
    }
    return ret;
  }

  private static Pair<File, String> parseNamedFileString(String curveString) {
    final String[] split = StringUtils.split(curveString, '=', 2);
    final File file = new File(split[0]);
    final String name;
    if (split.length > 1 && split[1].length() > 0) {
      name = split[1];
    } else {
      name = defaultNameFromFile(file);
    }
    return new Pair<>(file, name);
  }


  static File getPngFilename(File pngFile) {
    if (!pngFile.getName().endsWith(PNG_EXTENSION)) {
      return new File(pngFile.getParent(), pngFile.getName() + PNG_EXTENSION);
    }
    return pngFile;
  }

  private static void extractNamedFiles(Flag curveFlag, Flag fileFlag, ArrayList<File> fileList, ArrayList<String> nameList) {
    final List<Pair<File, String>> curveList = parseNamedFileStrings(curveFlag.getValues());
    for (Pair<File, String> filenamepair : curveList) {
      fileList.add(filenamepair.getA());
      nameList.add(filenamepair.getB());
    }
    for (final Object o : fileFlag.getValues()) {
      final File file = (File) o;
      fileList.add(file);
      nameList.add(defaultNameFromFile(file));
    }
    assert fileList.size() == nameList.size();
  }

  @Override
  public String moduleName() {
    return "rocplot";
  }

  @Override
  public String description() {
    return "plot ROC curves from vcfeval ROC data files";
  }

}

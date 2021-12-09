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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.reeltwo.plot.Box2D;
import com.reeltwo.plot.ui.ImageWriter;
import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;

/**
 */
public class RocPlotCli extends AbstractCli {

  private static final String HIDE_SIDEPANE_FLAG = "hide-sidepane";
  static final String PNG_FLAG = "png";
  static final String SVG_FLAG = "svg";
  static final String TITLE_FLAG = "title";
  static final String SCORES_FLAG = "scores";
  static final String POINTS_FLAG = "points";
  static final String PLAIN_FLAG = "plain";
  static final String INTERPOLATE_FLAG = "interpolate";
  static final String UNWEIGHTED_FLAG = "Xunweighted";
  static final String LINE_WIDTH_FLAG = "line-width";
  static final String CURVE_FLAG = "curve";
  static final String PRECISION_SENSITIVITY_FLAG = "precision-sensitivity";
  static final String ZOOM_FLAG = "zoom";
  static final String PALETTE = "palette";

  static final String SHOW_CMD_FLAG = "cmd";

  static final String PNG_EXTENSION = ".png";
  static final String SVG_EXTENSION = ".svg";

  private Flag<String> mCurveFlag;
  private Flag<File> mFileFlag;

  private static class FlagValidator implements Validator {
    @Override
    @SuppressWarnings("unchecked")
    public boolean isValid(CFlags flags) {
      if ((flags.isSet(PNG_FLAG) || flags.isSet(SVG_FLAG))
        && flags.getAnonymousValues(0).isEmpty() && !flags.isSet(CURVE_FLAG)) {
        // Image output requires a curve file
        // GUI mode doesn't, as the user can load after the app starts)
        flags.setParseMessage("Must supply at least 1 ROC file");
        return false;
      }
      if (flags.isSet(CURVE_FLAG)) {
        for (Pair<File, String> filepair : parseNamedFileStrings(((Flag<String>) flags.getFlag(CURVE_FLAG)).getValues())) {
          if (!CommonFlags.validateInputFile(flags, filepair.getA())) {
            return false;
          }
        }
      }
      for (Object o : flags.getAnonymousValues(0)) {
        if (!CommonFlags.validateInputFile(flags, (File) o)) {
          return false;
        }
      }
      if (flags.isSet(PNG_FLAG)) {
        if (!CommonFlags.validateOutputFile(flags, getImageFile((File) flags.getValue(PNG_FLAG), PNG_EXTENSION))) {
          return false;
        }
      }

      if (flags.isSet(SVG_FLAG)) {
        if (!CommonFlags.validateOutputFile(flags, getImageFile((File) flags.getValue(SVG_FLAG), SVG_EXTENSION))) {
          return false;
        }
      }
      if (!flags.checkInRange(LINE_WIDTH_FLAG, RocPlot.LINE_WIDTH_MIN, RocPlot.LINE_WIDTH_MAX)) {
        return false;
      }
      try {
        initialZoom(flags);
      } catch (IllegalArgumentException e) {
        flags.setParseMessage(e.getMessage());
        return false;
      }
      return true;
    }
  }

  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  private void initFlags(CFlags flags) {
    CommonFlagCategories.setCategories(flags);
    flags.setDescription("Plot ROC curves from vcfeval ROC data files, either to an image, or an interactive GUI.");
    CommonFlags.initForce(flags);
    flags.registerOptional('t', TITLE_FLAG, String.class, CommonFlags.STRING, "title for the plot").setCategory(REPORTING);
    flags.registerOptional(SCORES_FLAG, "if set, show scores on the plot").setCategory(REPORTING);
    flags.registerOptional(POINTS_FLAG, "if set, show points on each curve").setCategory(REPORTING);
    flags.registerOptional(PLAIN_FLAG, "if set, use a plain plot style").setCategory(REPORTING);
    flags.registerOptional(INTERPOLATE_FLAG, "if set, interpolate curves at regular intervals").setCategory(REPORTING);
    flags.registerOptional(UNWEIGHTED_FLAG, "if set, use unweighted TP on ROC").setCategory(REPORTING);
    flags.registerOptional(SHOW_CMD_FLAG, File.class, CommonFlags.FILE, "if set, print rocplot command used in previously saved image").setCategory(REPORTING);
    flags.registerOptional('P', PRECISION_SENSITIVITY_FLAG, "if set, plot precision vs sensitivity rather than ROC").setCategory(REPORTING);
    flags.registerOptional(HIDE_SIDEPANE_FLAG, "if set, hide the side pane from the GUI on startup").setCategory(REPORTING);
    flags.registerOptional(LINE_WIDTH_FLAG, Integer.class, CommonFlags.INT, "sets the plot line width", 2).setCategory(REPORTING);
    flags.registerOptional(ZOOM_FLAG, String.class, CommonFlags.STRING, "show a zoomed view with the given coordinates, supplied in the form <xmax>,<ymax> or <xmin>,<ymin>,<xmax>,<ymax>").setCategory(INPUT_OUTPUT);
    flags.registerOptional(PALETTE, String.class, CommonFlags.STRING, "name of color palette to use", RocPlotPalettes.SINGLETON.defaultName()).setParameterRange(RocPlotPalettes.SINGLETON.names()).setCategory(REPORTING);
    flags.registerOptional(PNG_FLAG, File.class, CommonFlags.FILE, "if set, output a PNG image to the given file").setCategory(INPUT_OUTPUT);
    flags.registerOptional(SVG_FLAG, File.class, CommonFlags.FILE, "if set, output a SVG image to the given file").setCategory(INPUT_OUTPUT);
    mCurveFlag = flags.registerOptional(CURVE_FLAG, String.class, CommonFlags.STRING, "ROC data file with title optionally specified (path[=title])").setCategory(INPUT_OUTPUT);
    mCurveFlag.setMinCount(0);
    mCurveFlag.setMaxCount(Integer.MAX_VALUE);
    mFileFlag = flags.registerRequired(File.class, CommonFlags.FILE, "ROC data file").setCategory(INPUT_OUTPUT);
    mFileFlag.setMinCount(0);
    mFileFlag.setMaxCount(Integer.MAX_VALUE);
    flags.addRequiredSet(mFileFlag);
    flags.addRequiredSet(mCurveFlag);
    flags.setValidator(new FlagValidator());
  }
  
  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final ArrayList<File> fileList = new ArrayList<>();
    final ArrayList<String> nameList = new ArrayList<>();
    extractNamedFiles(mCurveFlag, mFileFlag, fileList, nameList);

    if (mFlags.isSet(SHOW_CMD_FLAG)) {
      final File file = getImageFile((File) mFlags.getValue(SHOW_CMD_FLAG), PNG_EXTENSION);
      final String cmd = ImageWriter.getPngTextMetaData(file).get(RocPlotToFile.ROCPLOT_META_KEY);
      if (cmd == null) {
        Diagnostic.warning("No rocplot command metadata available");
      } else {
        Diagnostic.info(cmd);
      }
    } else if (mFlags.isSet(PNG_FLAG) || mFlags.isSet(SVG_FLAG)) {
      System.setProperty("java.awt.headless", "true");
      assert java.awt.GraphicsEnvironment.isHeadless() : "Something is messing with headless status before us!!";
      createImageIfFlagSet(fileList, nameList, PNG_FLAG, PNG_EXTENSION, ImageWriter.ImageFormat.PNG);
      createImageIfFlagSet(fileList, nameList, SVG_FLAG, SVG_EXTENSION, ImageWriter.ImageFormat.SVG);
    } else {   //Create and set up as a stand alone app.
      return RocPlot.mainExec(fileList, nameList, settingsFromFlags(new RocPlotSettings()), mFlags.isSet(HIDE_SIDEPANE_FLAG));
    }
    return 0;
  }

  private static Box2D initialZoom(CFlags flags) {
    if (flags.isSet(ZOOM_FLAG)) {
      final String[] parts = StringUtils.split((String) flags.getValue(ZOOM_FLAG), ',');
      try {
        switch (parts.length) {
          case 2:
            return new Box2D(0, 0, Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
          case 4:
            return new Box2D(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
          default:
            throw new IllegalArgumentException("Invalid zoom specifier: expected 4 numbers");
        }
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("Invalid zoom specifier", nfe);
      }
    } else {
      return null;
    }
  }

  private <T extends RocPlotSettings> T settingsFromFlags(T s) {
    s.setTitle((String) mFlags.getValue(TITLE_FLAG))
      .setShowScores(mFlags.isSet(SCORES_FLAG))
      .setShowPoints(mFlags.isSet(POINTS_FLAG))
      .setPlain(mFlags.isSet(PLAIN_FLAG))
      .setInterpolate(mFlags.isSet(INTERPOLATE_FLAG))
      .setWeighted(!mFlags.isSet(UNWEIGHTED_FLAG))
      .setLineWidth((Integer) mFlags.getValue(LINE_WIDTH_FLAG))
      .setPrecisionRecall(mFlags.isSet(PRECISION_SENSITIVITY_FLAG))
      .setInitialZoom(initialZoom(mFlags))
      .setPaletteName((String) mFlags.getValue(PALETTE));
    return s;
  }

  private void createImageIfFlagSet(ArrayList<File> fileList, ArrayList<String> nameList, String flagName, String fileExtension, ImageWriter.ImageFormat format) throws IOException {
    if (mFlags.isSet(flagName)) {
      final File file = getImageFile((File) mFlags.getValue(flagName), fileExtension);
      settingsFromFlags(new RocPlotToFile())
        .setImageFormat(format)
        .writeRocPlot(file, fileList, nameList);
    }
  }

  static List<Pair<File, String>> parseNamedFileStrings(List<String> curveStrings) {
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
      name = split[1].trim();
    } else {
      name = "";
    }
    return new Pair<>(file, name);
  }


  static File getImageFile(File outFile, String extension) {
    return FileUtils.getOutputFileName(outFile, false, extension);
  }

  private static void extractNamedFiles(Flag<String> curveFlag, Flag<File> fileFlag, ArrayList<File> fileList, ArrayList<String> nameList) {
    final List<Pair<File, String>> curveList = parseNamedFileStrings(curveFlag.getValues());
    for (Pair<File, String> filenamepair : curveList) {
      fileList.add(filenamepair.getA());
      nameList.add(filenamepair.getB());
    }
    for (final File file : fileFlag.getValues()) {
      fileList.add(file);
      nameList.add("");
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

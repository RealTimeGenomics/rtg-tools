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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.reeltwo.plot.Graph2D;
import com.reeltwo.plot.KeyPosition;
import com.reeltwo.plot.Plot2D;
import com.reeltwo.plot.Point2D;
import com.reeltwo.plot.PointPlot2D;
import com.reeltwo.plot.renderer.Mapping;
import com.reeltwo.plot.ui.PlotPanel;
import com.reeltwo.plot.ui.ZoomPlotPanel;
import com.rtg.util.ContingencyTable;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.eval.RocFilter;

/**
 * Starts a new Swing window for displaying {@code Graph2D}s in. The window has
 * zooming and picture in picture functionality enabled.
 *
 */
@JumbleIgnore
public class RocPlot {


  /** Minimum allowed line width */
  public static final int LINE_WIDTH_MIN = 1;
  /** Maximum allowed line width */
  public static final int LINE_WIDTH_MAX = 10;

  // File chooser stored user-preference keys
  private static final String CHOOSER_WIDTH = "chooser-width";
  private static final String CHOOSER_HEIGHT = "chooser-height";

  private final ProgressBarDelegate mProgressBarDelegate;

  private final JPanel mMainPanel;
  /** panel showing plot */
  private final PlotPanel mPlotPanel;
  private final RocZoomPlotPanel mZoomPP;
  /** a progress bar */
  private final JProgressBar mProgressBar;
  /** pop up menu */
  private final JPopupMenu mPopup;

  private final JLabel mIconLabel;

  private final RocLinesPanel mRocLinesPanel;
  private final JSlider mLineWidthSlider;
  private final JCheckBox mScoreCB;
  private final JCheckBox mSelectAllCB;
  private final JButton mOpenButton;
  private final JButton mCommandButton;
  private final JTextField mTitleEntry;
  private JSplitPane mSplitPane;
  private final JLabel mStatusLabel;

  // Graph data and state
  final Map<String, DataBundle> mData = Collections.synchronizedMap(new HashMap<String, DataBundle>());
  boolean mShowScores = true;
  int mLineWidth = 2;

  private float mMaxXHi = -1.0f;
  private float mMaxYHi = -1.0f;

  private final JScrollPane mScrollPane;

  private final JFileChooser mFileChooser = new JFileChooser();
  private File mFileChooserParent = null;

  private static final Color[] PALETTE = {
    new Color(0xFF4030),
    new Color(0x30F030),
    new Color(0x3030FF),
    new Color(0xFF30FF),
    new Color(0x30FFFF),
    new Color(0xA05050),
    new Color(0xF0C040),
    new Color(0x707070),
    new Color(0xC00000),
    new Color(0x00C000),
    new Color(0x0000C0),
    new Color(0xC000C0),
    new Color(0x00C0C0),
    new Color(0xB0B0B0),
  };

  /** Creates a new swing plot. */
  RocPlot() {
    mMainPanel = new JPanel();
    mPlotPanel = new PlotPanel(true);
    mPlotPanel.setColors(PALETTE);
    mZoomPP = new RocZoomPlotPanel(mPlotPanel, mMainPanel);
    mZoomPP.setOriginIsMin(true);
    mProgressBar = new JProgressBar(-1, -1);
    mProgressBar.setVisible(true);
    mProgressBar.setStringPainted(true);
    mProgressBar.setIndeterminate(true);
    mProgressBarDelegate = new ProgressBarDelegate(mProgressBar);
    mStatusLabel = new JLabel();
    mPopup = new JPopupMenu();
    mRocLinesPanel = new RocLinesPanel(this);
    mScrollPane = new JScrollPane(mRocLinesPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    mScrollPane.setWheelScrollingEnabled(true);
    mLineWidthSlider = new JSlider(JSlider.HORIZONTAL, LINE_WIDTH_MIN, LINE_WIDTH_MAX, 1);
    mScoreCB = new JCheckBox("Show Scores");
    mScoreCB.setSelected(true);
    mSelectAllCB = new JCheckBox("Select / Deselect all");
    mTitleEntry = new JTextField("ROC");
    mTitleEntry.setMaximumSize(new Dimension(Integer.MAX_VALUE, mTitleEntry.getPreferredSize().height));
    mOpenButton = new JButton("Open...");
    mOpenButton.setToolTipText("Add a new ROC curve from a file");
    mCommandButton = new JButton("Cmd");
    mCommandButton.setToolTipText("Send the equivalent rocplot command-line to the terminal");
    final ImageIcon icon = createImageIcon("com/rtg/graph/resources/realtimegenomics_logo.png", "RTG Logo");
    mIconLabel = new JLabel(icon);
    mIconLabel.setBackground(new Color(16, 159, 205));
    mIconLabel.setForeground(Color.WHITE);
    mIconLabel.setOpaque(true);
    mIconLabel.setFont(new Font("Arial", Font.BOLD, 24));
    mIconLabel.setHorizontalAlignment(JLabel.LEFT);
    mIconLabel.setIconTextGap(50);
    configureUI();
  }

  protected static ImageIcon createImageIcon(String path, String description) {
    final java.net.URL imgURL = Resources.getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL, description);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }

  /**
   * Layout and show the GUI.
   */
  private void configureUI() {
    mMainPanel.setLayout(new BorderLayout());
    final JPanel pane = new JPanel(new BorderLayout());
    pane.add(mPlotPanel, BorderLayout.CENTER);
    final JPanel rightPanel = new JPanel(new GridBagLayout());
    mSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pane, rightPanel);
    mSplitPane.setContinuousLayout(true);
    mSplitPane.setOneTouchExpandable(true);
    mSplitPane.setResizeWeight(1);

    mMainPanel.add(mSplitPane, BorderLayout.CENTER);
    mMainPanel.add(mStatusLabel, BorderLayout.SOUTH);

    mPopup.setLightWeightPopupEnabled(false);
    mPopup.add(mZoomPP.getZoomOutAction());
    mPopup.addSeparator();
    mPopup.add(mPlotPanel.getPrintAction());
    mPopup.add(mPlotPanel.getSaveImageAction());
    mPopup.add(mPlotPanel.getSnapShotAction());
    mPopup.addSeparator();

    mPlotPanel.addMouseListener(new PopupListener());

    mPlotPanel.setBackground(Color.WHITE);
    mPlotPanel.setGraphBGColor(new Color(0.8f, 0.9f, 1.0f), Color.WHITE);
    mPlotPanel.setGraphShadowWidth(4);

    final GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(2, 2, 2, 2);
    c.gridx = 0;
    c.weightx = 1;    c.weighty = 0;
    c.fill = GridBagConstraints.HORIZONTAL;    c.anchor = GridBagConstraints.CENTER;
    rightPanel.add(new JLabel("Title", JLabel.CENTER), c);
    mTitleEntry.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          mTitleEntry.setText(mIconLabel.getText()); // revert edit
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          setTitle(mTitleEntry.getText());
          showCurrentGraph();
        }
      }

    });
    mTitleEntry.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        setTitle(mTitleEntry.getText());
        showCurrentGraph();
      }
    });
    rightPanel.add(mTitleEntry, c);

    rightPanel.add(new JLabel("Line Width", JLabel.CENTER), c);
    mLineWidthSlider.setSnapToTicks(true);
    mLineWidthSlider.setValue(mLineWidth);
    mLineWidthSlider.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        mLineWidth = mLineWidthSlider.getValue();
        showCurrentGraph();
      }
    });
    rightPanel.add(mLineWidthSlider, c);

    mScoreCB.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        mShowScores = mScoreCB.isSelected();
        showCurrentGraph();
      }
    });
    mScoreCB.setAlignmentX(0);
    rightPanel.add(mScoreCB, c);

    mSelectAllCB.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        for (final Component component : mRocLinesPanel.getComponents()) {
          final RocLinePanel cp = (RocLinePanel) component;
          cp.setSelected(mSelectAllCB.isSelected());
        }
      }
    });
    mSelectAllCB.setSelected(true);
    mOpenButton.addActionListener(new LoadFileListener());
    mCommandButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.out.println("Equivalent rocplot command:\n" + getCommand() + "\n");
      }
    });

    final JPanel namePanel = new JPanel(new GridBagLayout());
    c.fill = GridBagConstraints.NONE;           c.anchor = GridBagConstraints.LINE_START;
    rightPanel.add(mOpenButton, c);
    rightPanel.add(mSelectAllCB, c);
    c.fill = GridBagConstraints.HORIZONTAL;     c.anchor = GridBagConstraints.CENTER;

    final GridBagConstraints scrollConstraints = new GridBagConstraints();
    scrollConstraints.gridx = 0; scrollConstraints.gridy = 1;
    scrollConstraints.weightx = 2; scrollConstraints.weighty = 2;
    scrollConstraints.fill = GridBagConstraints.BOTH;
    mRocLinesPanel.setPreferredSize(new Dimension(mScrollPane.getViewport().getViewSize().width, mRocLinesPanel.getPreferredSize().height));
    namePanel.add(mScrollPane, scrollConstraints);

    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    rightPanel.add(namePanel, c);

    c.weighty = 0;
    c.fill = GridBagConstraints.NONE;    c.anchor = GridBagConstraints.FIRST_LINE_START;
    final Box b = Box.createHorizontalBox();
    b.add(mOpenButton);
    b.add(mCommandButton);
    rightPanel.add(b, c);

    pane.add(mProgressBar, BorderLayout.SOUTH);

    mIconLabel.setText(mTitleEntry.getText());
    pane.add(mIconLabel, BorderLayout.NORTH);
  }

  @JumbleIgnore
  private class LoadFileListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (mFileChooserParent != null) {
        mFileChooser.setCurrentDirectory(mFileChooserParent);
      }

      final Preferences prefs = Preferences.userNodeForPackage(RocPlot.this.getClass());
      if (prefs.getInt(CHOOSER_WIDTH, -1) != -1 && prefs.getInt(CHOOSER_HEIGHT, -1) != -1) {
        mFileChooser.setPreferredSize(new Dimension(prefs.getInt(CHOOSER_WIDTH, 640), prefs.getInt(CHOOSER_HEIGHT, 480)));
      }
      if (mFileChooser.showOpenDialog(mMainPanel) == JFileChooser.APPROVE_OPTION) {
        final File f = mFileChooser.getSelectedFile();
        if (f != null) {
          try {
            loadFile(f, null, true);
            updateProgress();
            showCurrentGraph();
          } catch (final IOException | NoTalkbackSlimException e1) {
            JOptionPane.showMessageDialog(mMainPanel,
              "Could not open file: " + f.getPath() + "\n"
                + (e1.getMessage().length() > 100 ? e1.getMessage().substring(0, 100) + "..." : e1.getMessage()),
              "Invalid ROC File", JOptionPane.ERROR_MESSAGE);
          }
          final Dimension r = mFileChooser.getSize();
          prefs.putInt(CHOOSER_WIDTH, (int) r.getWidth());
          prefs.putInt(CHOOSER_HEIGHT, (int) r.getHeight());
        }
      }
    }
  }

  private String getCommand() {
    final StringBuilder sb = new StringBuilder("rtg rocplot ");
    sb.append("--").append(RocPlotCli.LINE_WIDTH_FLAG).append(' ').append(mLineWidth);
    if (mShowScores) {
      sb.append(" --").append(RocPlotCli.SCORES_FLAG);
    }
    sb.append(" --").append(RocPlotCli.TITLE_FLAG).append(' ').append(StringUtils.dumbQuote(mTitleEntry.getText()));
    for (final Component component : mRocLinesPanel.getComponents()) {
      final RocLinePanel cp = (RocLinePanel) component;
      if (cp.isSelected()) {
        sb.append(" --").append(RocPlotCli.CURVE_FLAG).append(" ").append(StringUtils.dumbQuote(cp.getPath() + "=" + cp.getLabel()));
      }
    }
    return sb.toString();
  }

  // Adds the notion of painting a current crosshair position
  @JumbleIgnore
  private static class RocZoomPlotPanel extends ZoomPlotPanel {
    private final PlotPanel mPlotPanel;
    private Point mCrosshair; // In TP / FP coordinates.
    RocZoomPlotPanel(PlotPanel plotPanel, Container container) {
      super(plotPanel, container);
      mPlotPanel = plotPanel;
    }
    @Override
    public void paint(Graphics g) {
      super.paint(g);
      final Mapping[] mapping = mPlotPanel.getMapping();
      if (mapping != null && mapping.length > 1 && mCrosshair != null) {
        Point p = new Point((int) mapping[0].worldToScreen(mCrosshair.x), (int) mapping[1].worldToScreen(mCrosshair.y));
        final boolean inView = p.x >= mapping[0].getScreenMin() && p.x <= mapping[0].getScreenMax()
          && p.y <= mapping[1].getScreenMin() && p.y >= mapping[1].getScreenMax(); // Y screen min/max is inverted due to coordinate system
        if (inView) {
          p = SwingUtilities.convertPoint(mPlotPanel, p, this);
          g.setColor(Color.BLACK);
          final int size = 9;
          g.drawLine(p.x - size, p.y - size, p.x + size, p.y + size);
          g.drawLine(p.x - size, p.y + size, p.x + size, p.y - size);
        }
      }
    }
    void setCrossHair(Point p) {
      mCrosshair = p;
    }
  }

  // Adds the notion of the baseline total number of variants, for calculating sensitivity
  @JumbleIgnore
  static class RocGraph2D extends Graph2D {
    private final int mMaxVariants;
    RocGraph2D(ArrayList<String> lineOrdering, int lineWidth, boolean showScores, Map<String, DataBundle> data, String title) {
      setKeyVerticalPosition(KeyPosition.BOTTOM);
      setKeyHorizontalPosition(KeyPosition.RIGHT);
      setGrid(true);
      setLabel(Graph2D.Y, "True Positives");
      setLabel(Graph2D.X, "False Positives");
      setTitle(title);

      int maxVariants = -1;
      for (int i = 0; i < lineOrdering.size(); i++) {
        final DataBundle db = data.get(lineOrdering.get(i));
        if (db.show()) {
          addPlot(db.getPlot(lineWidth, i));
          if (showScores) {
            addPlot(db.getScorePoints(lineWidth, i));
            addPlot(db.getScoreLabels());
          }
          if (db.getTotalVariants() > maxVariants) {
            maxVariants = db.getTotalVariants();
          }
        }
      }

      if (maxVariants > 0) {
        setRange(Graph2D.Y, 0, maxVariants);
        setTitle(title + " (baseline total = " + maxVariants + ")");

        setRange(Graph2D.Y, Graph2D.TWO, 0, 100);
        setShowTics(Graph2D.Y, Graph2D.TWO, true);
        setGrid(Graph2D.Y, Graph2D.TWO, false);
        setLabel(Graph2D.Y, Graph2D.TWO, "%");
        // dummy plot to show Y2 axis
        final PointPlot2D pp = new PointPlot2D(Graph2D.ONE, Graph2D.TWO);
        addPlot(pp);
      }
      mMaxVariants = maxVariants;
    }

    public int getMaxVariants() {
      return mMaxVariants;
    }
  }

  void showCurrentGraph() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final Graph2D graph = new RocGraph2D(RocPlot.this.mRocLinesPanel.plotOrder(), RocPlot.this.mLineWidth, RocPlot.this.mShowScores, RocPlot.this.mData, RocPlot.this.mTitleEntry.getText());
        maintainZoomMax(graph);
        graph.addPlot(invisibleGraph());
        mZoomPP.setGraph(graph, true);
      }
    });
  }

  private Plot2D invisibleGraph() {
    // Invisible graph to maintain graph size when no lines are shown
    final PointPlot2D plot = new PointPlot2D();
    plot.setData(Arrays.asList(new Point2D(0, 0), new Point2D(mMaxXHi, mMaxYHi)));
    plot.setLines(false);
    plot.setPoints(false);
    return plot;
  }

  private void maintainZoomMax(Graph2D graph) {
    mMaxXHi = Math.max(mMaxXHi, graph.getHi(Graph2D.X, Graph2D.ONE));
    mMaxYHi = Math.max(mMaxYHi, graph.getHi(Graph2D.Y, Graph2D.ONE));
  }

  void updateProgress() {
    mProgressBarDelegate.done();
  }

  public ProgressBarDelegate getProgressBarDelegate() {
    return mProgressBarDelegate;
  }


  /**
   * Set the title of the plot
   * @param title plot title
   */
  public void setTitle(final String title) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mIconLabel.setText(title);
        mTitleEntry.setText(title);
      }
    });
  }

  /**
   * Set whether to show scores on the plot lines
   * @param flag show scores
   */
  public void showScores(boolean flag) {
    mShowScores = flag;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mScoreCB.setSelected(mShowScores);
      }
    });
  }

  /**
   * Set whether to show the open file button
   * @param flag show open file button
   */
  public void showOpenButton(boolean flag) {
    mOpenButton.setVisible(flag);
  }

  /**
   * Set whether to show the command dump button
   * @param flag show command dump button
   */
  public void showCommandButton(boolean flag) {
    mCommandButton.setVisible(flag);
  }

  /**
   * Set the line width slider to the given value
   * @param width line width
   */
  public void setLineWidth(int width) {
    mLineWidth = width < LINE_WIDTH_MIN ? LINE_WIDTH_MIN : width > LINE_WIDTH_MAX ? LINE_WIDTH_MAX : width;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mLineWidthSlider.setValue(mLineWidth);
      }
    });
  }

  /**
   * Sets the split pane divider location
   * @param loc proportional location
   */
  public void setSplitPaneDividerLocation(double loc) {
    mSplitPane.setDividerLocation(loc);
  }

  /**
   * Returns the main application panel
   * @return main panel
   */
  public JPanel getMainPanel() {
    return mMainPanel;
  }

  /**
   * Returns the zooming part of the plot panel
   * @return zoom plot panel
   */
  public ZoomPlotPanel getZoomPlotPanel() {
    return mZoomPP;
  }

  /**
   * Set a status message
   * @param message test to display
   */
  public void setStatus(String message) {
    mStatusLabel.setText(message);
  }

  private void loadData(ArrayList<File> files, ArrayList<String> names, boolean showProgress) throws IOException {
    for (int i = 0; i < files.size(); i++) {
      final File f = files.get(i);
      final String name = names.get(i);
      loadFile(f, name, showProgress);
    }
    if (showProgress) {
      updateProgress();
    }
  }

  private void loadFile(final File f, final String name, boolean showProgress) throws IOException {
    mFileChooserParent = f.getParentFile();
    final DataBundle data = ParseRocFile.loadStream(mProgressBarDelegate, FileUtils.createInputStream(f, false), f.getCanonicalPath(), showProgress);
    if (name != null && name.trim().length() == 0) {
      data.setTitle(name);
    } else {
      final StringBuilder autoname = new StringBuilder();
      autoname.append(f.getCanonicalFile().getParentFile().getName());

      final String path = f.getCanonicalFile().getName();
      final int rocIdx = path.indexOf(RocFilter.ROC_EXT);
      if (rocIdx != -1 && !path.startsWith(RocFilter.ALL.fileName())) {
        if (autoname.length() > 0) {
          autoname.append(' ');
        }
        autoname.append(path.substring(0, rocIdx));
      }

      if (data.getScoreName() != null) {
        if (autoname.length() > 0) {
          autoname.append(' ');
        }
        autoname.append(data.getScoreName());
      }
      data.setTitle(autoname.toString());
    }
    addLine(f.getAbsolutePath(), data);
  }

  private void addLine(String path, DataBundle dataBundle) {
    mData.put(path, dataBundle);
    mRocLinesPanel.addLine(new RocLinePanel(this, path, dataBundle.getTitle(), dataBundle, mProgressBar));
    showCurrentGraph();
  }

  /**
   * A class required to listen for right-clicks
   */
  @JumbleIgnore
  private class PopupListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      final Point p = e.getPoint();
      final Mapping[] mapping = mPlotPanel.getMapping();
      final int maxVariants = ((RocGraph2D) mPlotPanel.getGraph()).getMaxVariants();
      if (mapping != null && mapping.length > 1) {
        final boolean inView = p.x >= mapping[0].getScreenMin() && p.x <= mapping[0].getScreenMax()
          && p.y <= mapping[1].getScreenMin() && p.y >= mapping[1].getScreenMax(); // Y screen min/max is inverted due to coordinate system
        final float fp = mapping[0].screenToWorld((float) p.getX());
        final float tp = mapping[1].screenToWorld((float) p.getY());
        if (inView && fp >= 0 && tp >= 0 && (fp + tp > 0)) {
          mProgressBar.setString(getMetricString(tp, fp, maxVariants));
          mZoomPP.setCrossHair(new Point((int) fp, (int) tp));
        } else {
          mZoomPP.setCrossHair(null);
          mProgressBar.setString("");
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        mPopup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  static String getMetricString(double truePositive, double falsePositive, int totalPositive) {
    final double precision = ContingencyTable.precision(truePositive, falsePositive);
    String message = String.format("TP=%.0f FP=%.0f Precision=%.2f%%", truePositive, falsePositive, precision * 100);
    if (totalPositive > 0) {
      final double falseNegative = totalPositive - truePositive;
      final double recall = ContingencyTable.recall(truePositive, falseNegative);
      final double fMeasure = ContingencyTable.fMeasure(precision, recall);
      message += String.format(" Sensitivity=%.2f%% F-measure=%.2f%%", recall * 100, fMeasure * 100);
    }
    return message;
  }


  void rocStandalone(ArrayList<File> fileList, ArrayList<String> nameList, String title, boolean scores, final boolean hideSidePanel, int lineWidth) throws InterruptedException, InvocationTargetException, IOException {
    final JFrame frame = new JFrame();
    final RocPlot rp = new RocPlot() {
      @Override
      public void setTitle(final String title) {
        super.setTitle(title);
        frame.setTitle("rtg rocplot - " + title);
      }
    };
    rp.setLineWidth(lineWidth);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(rp.mMainPanel, BorderLayout.CENTER);
    frame.setGlassPane(rp.mZoomPP);
    frame.getGlassPane().setVisible(true);
    final CountDownLatch lock = new CountDownLatch(1);
    rp.mPopup.add(new AbstractAction("Exit", null) {
      @Override
      public void actionPerformed(ActionEvent e) {
        frame.setVisible(false);
        frame.dispose();
        lock.countDown();
      }
    });
    rp.showScores(scores);
    rp.setTitle(title == null ? "ROC" : title);
    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocation(50, 50);
        frame.setVisible(true);
        rp.showCurrentGraph();
        if (hideSidePanel) {
          rp.setSplitPaneDividerLocation(1.0);
        }
      }
    });
    rp.loadData(fileList, nameList, true);
    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        rp.mZoomPP.setGraph(new RocGraph2D(rp.mRocLinesPanel.plotOrder(), rp.mLineWidth, rp.mShowScores, rp.mData, rp.mTitleEntry.getText()), false);
      }
    });
    lock.await();
  }
}

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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.reeltwo.plot.TextPoint2D;
import com.rtg.util.Environment;

/**
 * A panel that lets you enable or disable showing of a ROC line, rename the ROC line, and has buttons for
 * moving the line up or down in the list.
 */
@JumbleIgnore
class RocLinePanel extends JPanel {

  private final JCheckBox mCheckBox;
  private final DataBundle mDataBundle;
  private final String mPath;
  private final JTextField mTextField;
  private final RocPlot mRocPlot;
  private final JProgressBar mStatusBar;
  private final JPopupMenu mPopup = new JPopupMenu();
  private final JMenuItem mRemoveItem;
  private Color mColor;
  private final JPanel mGripper;

  RocLinePanel(RocPlot rocPlot, final String path, final String name, DataBundle data, JProgressBar statusBar, Color color) {
    super(new GridBagLayout());
    mRocPlot = rocPlot;
    mPath = path;
    mDataBundle = data;
    mColor = color;
    mCheckBox = new JCheckBox() {
      // Needed to make alpha channel repainting of checkbox work
      @Override
      protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
      }
    };
    mCheckBox.setSelected(true);
    mCheckBox.setBackground(color);
    mCheckBox.setOpaque(false);
    mTextField = new JTextField();
    mTextField.setText(name);
    mStatusBar = statusBar;
    mTextField.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        mStatusBar.setString(getTooltip(mPath, mTextField.getText()));
      }
    });

    mTextField.addFocusListener(new FocusAdapter() {

      @Override
      public void focusLost(FocusEvent e) {
        mDataBundle.setTitle(mTextField.getText());
        mRocPlot.showCurrentGraph();
      }
    });


    mTextField.addKeyListener(new KeyAdapter() {

      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          mTextField.setText(mDataBundle.getTitle());
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          mDataBundle.setTitle(mTextField.getText());
          mRocPlot.showCurrentGraph();
        }
      }

    });

    final Box textPanel = Box.createHorizontalBox();
    final Dimension d = new Dimension(10, 0);
    mGripper = new JPanel();
    mGripper.setPreferredSize(d);
    mGripper.setBackground(mColor);
    mGripper.setBorder(new BevelBorder(BevelBorder.RAISED));
    textPanel.add(mGripper);
    textPanel.add(mCheckBox);
    textPanel.add(mTextField);
    final GridBagConstraints textConstraints = new GridBagConstraints();
    textConstraints.gridx = 0; textConstraints.gridy = 0;
    textConstraints.fill = GridBagConstraints.HORIZONTAL; textConstraints.weightx = 2;
    add(textPanel, textConstraints);
    mCheckBox.addItemListener(new CheckBoxListener(this));
    setBorder(new EmptyBorder(1, 1, 1, 1)); // Placeholder border, so we can draw one in drag and drop

    final JSlider rangeSlider = new JSlider();
    rangeSlider.setMinimum(0);
    rangeSlider.setMaximum(1000);
    rangeSlider.setValue(1000);

    if (Environment.OS_MAC_OS_X) {
      RightMouseButtonFilter.insertRightMouseButtonFilter(rangeSlider);
    }

    rangeSlider.addChangeListener(e -> {
      final JSlider slider = (JSlider) e.getSource();
      mDataBundle.setScoreRange(0.0f, slider.getValue() / 1000.0f);
      mRocPlot.showCurrentGraph();
      final TextPoint2D data1 = mDataBundle.getMaxRangedPoint();
      if (data1 != null) {
        mStatusBar.setString(RocPlot.getMetricString(data1.getY(), data1.getX(), mDataBundle.getTotalVariants()) + " Threshold=" + data1.getText());
      }
    });

    final GridBagConstraints rangeConstraints = new GridBagConstraints();
    rangeConstraints.gridx = 0; rangeConstraints.gridy = 1;
    rangeConstraints.gridwidth = 2; rangeConstraints.fill = GridBagConstraints.HORIZONTAL;
    add(rangeSlider, rangeConstraints);

    mRemoveItem = new JMenuItem("Remove");
    mRemoveItem.setActionCommand("remove");
    final JMenuItem colorChooseItem = new JMenuItem("Color...");
    colorChooseItem.addActionListener(e -> {
      final Color newColor = JColorChooser.showDialog(RocLinePanel.this, "Choose line color", mColor);
      if (newColor != null) {
        RocLinePanel.this.setColor(newColor);
      }
    });
    mPopup.add(colorChooseItem);
    mPopup.add(mRemoveItem);
    final MouseListener l = new MouseAdapter() {
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
    };
    addMouseListener(l);
    rangeSlider.addMouseListener(l);
    mCheckBox.addMouseListener(l);
    mTextField.addMouseListener(l);

    setMaximumSize(new Dimension(getMaximumSize().width, getPreferredSize().height));
  }

  public Color getColor() {
    return mColor;
  }

  private static class CheckBoxListener implements ItemListener {
    private final RocLinePanel mPanel;

    CheckBoxListener(RocLinePanel panel) {
      mPanel = panel;
    }
    @Override
    public void itemStateChanged(ItemEvent e) {
      mPanel.setSelected(mPanel.mCheckBox.isSelected());
    }
  }

  private String getTooltip(String path, String name) {
    return name + "=" + path;
  }

  public void setSelected(boolean selected) {
    mDataBundle.show(selected);
    mCheckBox.setSelected(selected);
    mRocPlot.showCurrentGraph();
  }

  public boolean isSelected() {
    return mCheckBox.isSelected();
  }

  public String getPath() {
    return mPath;
  }

  public String getLabel() {
    return mTextField.getText();
  }

  public void addActionListener(ActionListener listener) {
    mRemoveItem.addActionListener(listener);
  }

  void setColor(final Color color) {
    mColor = color;
    mGripper.setBackground(color);
    mCheckBox.setBackground(color);
    repaint();
    mRocPlot.showCurrentGraph();
  }

  public void addReorderListener(final MouseAdapter listener) {
    addMouseListener(listener);
    addMouseMotionListener(listener);
  }

  /**
   * Filter all right mouse button events.  We use this to get around a bug where
   * the JSlider responds to right mouse button events on the mac.
   */
  private static final class RightMouseButtonFilter implements MouseListener {

    private final MouseListener[] mListeners;

    private RightMouseButtonFilter(final MouseListener[] listeners) {
      mListeners = listeners;
    }

    private static void insertRightMouseButtonFilter(final JComponent component) {
      final RightMouseButtonFilter filter = new RightMouseButtonFilter(component.getMouseListeners());
      for (final MouseListener l : filter.mListeners) {
        component.removeMouseListener(l);
      }
      component.addMouseListener(filter);
    }

    private boolean isRetained(final MouseEvent e) {
      return !SwingUtilities.isRightMouseButton(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (isRetained(e)) {
        for (final MouseListener u : mListeners) {
          u.mouseClicked(e);
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (isRetained(e)) {
        for (final MouseListener u : mListeners) {
          u.mousePressed(e);
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (isRetained(e)) {
        for (final MouseListener u : mListeners) {
          u.mouseReleased(e);
        }
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      if (isRetained(e)) {
        for (final MouseListener u : mListeners) {
          u.mouseEntered(e);
        }
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      if (isRetained(e)) {
        for (final MouseListener u : mListeners) {
          u.mouseExited(e);
        }
      }
    }
  }
}

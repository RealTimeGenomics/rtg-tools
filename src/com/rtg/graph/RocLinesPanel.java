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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import com.reeltwo.jumble.annotations.JumbleIgnore;

/**
 * A panel that holds a bunch of ROC line UI panels, handles changing the order of lines in the list.
 */
@JumbleIgnore
class RocLinesPanel extends Box {

  private final RocPlot mRocPlot;
  private final ArrayList<String> mPlotOrder = new ArrayList<>();

  RocLinesPanel(RocPlot rocPlot) {
    super(BoxLayout.Y_AXIS);
    mRocPlot = rocPlot;
  }

  ArrayList<String> plotOrder() {
    return mPlotOrder;
  }

  public void addLine(final RocLinePanel cp) {
    cp.addActionListener(new MoveActionListener(cp));
    cp.addReorderListener(new ReorderListener(cp));
    SwingUtilities.invokeLater(() -> {
      mPlotOrder.add(cp.getPath());
      add(cp);
    });
  }

  @Override
  public Component add(Component comp) {
    final Component add = super.add(comp);
    int sizey = 0;
    for (Component c : getComponents()) {
      sizey += c.getPreferredSize().height;
    }
    setPreferredSize(new Dimension(getPreferredSize().width, sizey));
    return add;
  }

  @JumbleIgnore
  private class MoveActionListener implements ActionListener {
    private final RocLinePanel mPanel;

    MoveActionListener(RocLinePanel panel) {
      mPanel = panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final Component[] components = getComponents();
      for (final Component component : components) {
        final RocLinePanel cp = (RocLinePanel) component;
        if (cp == mPanel) {
          switch (e.getActionCommand()) {
            case "remove":
              remove(mPanel);
              mRocPlot.mData.remove(mPanel.getPath());
              break;
            default:
              System.err.println("Unhandled event!");
          }
          updateCurves();
          break;
        }
      }
    }
  }

  private void updateCurves() {
    revalidate();
    repaint();
    mPlotOrder.clear();
    for (Component cp : getComponents()) {
      mPlotOrder.add(((RocLinePanel) cp).getPath());
    }
    mRocPlot.showCurrentGraph();
  }

  private class ReorderListener extends MouseAdapter {

    private int mOriginalScreenY;
    private int mOriginalY;
    private final RocLinePanel mPanel;
    private final Border mBorder;
    private boolean mIsDragging = false;

    ReorderListener(final RocLinePanel panel) {
      mPanel = panel;
      mBorder = mPanel.getBorder();
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
        if (mIsDragging) {
          mouseReleased(e); // Somehow clicked left mouse again while dragging?
        } else if (!e.isPopupTrigger()) {
          mIsDragging = true;
          mPanel.setBorder(LineBorder.createGrayLineBorder());
          setComponentZOrder(mPanel, 0); // Also moves this item to component 0
          mOriginalScreenY = e.getYOnScreen();
          mOriginalY = mPanel.getY();
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (mIsDragging && e.getButton() == MouseEvent.BUTTON1) {
        final int deltaY = e.getYOnScreen() - mOriginalScreenY;
        final int newY = mOriginalY + deltaY;
        final Component[] components = getComponents();
        // Find new position for mPanel (it is currently at position 0 due to Z order)
        int i;
        for (i = 1; i < components.length; ++i) {
          if (components[i].getY() >= newY) {
            break;
          }
        }
        mPanel.setBorder(mBorder);
        remove(mPanel);
        add(mPanel, i - 1);
        updateCurves();
        mIsDragging = false;
      }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      if (mIsDragging) {
        final int deltaY = e.getYOnScreen() - mOriginalScreenY;
        mPanel.setLocation(mPanel.getX(), mOriginalY + deltaY);
      }
    }
  }}

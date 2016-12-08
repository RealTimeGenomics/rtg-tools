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
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;

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
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mPlotOrder.add(cp.getPath());
        add(cp);
      }
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
      for (int i = 0; i < components.length; ++i) {
        final RocLinePanel cp = (RocLinePanel) components[i];

        if (cp == mPanel) {
          if ("up".equals(e.getActionCommand())) {
            //System.err.println("Move up ");
            if (i > 0) {
              mPlotOrder.remove(i);
              mPlotOrder.add(i - 1, mPanel.getPath());
              remove(mPanel);
              add(mPanel, i - 1);
            }
          } else {
            //System.err.println("Move down ");
            if (i < components.length - 1) {
              mPlotOrder.remove(i);
              mPlotOrder.add(i + 1, mPanel.getPath());
              remove(mPanel);
              add(mPanel, i + 1);
            }
          }
          validate();
          mRocPlot.showCurrentGraph();
          break;
        }
      }
    }
  }

}

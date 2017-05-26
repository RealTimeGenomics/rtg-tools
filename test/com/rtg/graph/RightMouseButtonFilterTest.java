/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JPanel;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class RightMouseButtonFilterTest extends TestCase {

  public void test() {
    final ArrayList<String> s = new ArrayList<>();
    final RocLinePanel.RightMouseButtonFilter f = new RocLinePanel.RightMouseButtonFilter(new MouseListener[] {new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        s.add("mouseClicked");
      }

      @Override
      public void mousePressed(MouseEvent e) {
        s.add("mousePressed");
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        s.add("mouseReleased");
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        s.add("mouseEntered");
      }

      @Override
      public void mouseExited(MouseEvent e) {
        s.add("mouseExited");
      }
    }});
    final MouseEvent left = new MouseEvent(new JPanel(), 0, 0, 0, 0, 0, 0, false, MouseEvent.BUTTON1);
    f.mouseClicked(left);
    f.mousePressed(left);
    f.mouseReleased(left);
    f.mouseEntered(left);
    f.mouseExited(left);
    assertEquals("[mouseClicked, mousePressed, mouseReleased, mouseEntered, mouseExited]", s.toString());
    // The following all get gobbled
    final MouseEvent right = new MouseEvent(new JPanel(), 0, 0, 0, 0, 0, 0, false, MouseEvent.BUTTON3);
    f.mouseClicked(right);
    f.mousePressed(right);
    f.mouseReleased(right);
    f.mouseEntered(right);
    f.mouseExited(right);
    assertEquals("[mouseClicked, mousePressed, mouseReleased, mouseEntered, mouseExited]", s.toString());
  }

}

/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.dataview.dvmodeler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

/**
 *
 * @author Andriy Shapochka
 * @version 1.0
 */

public class JTile extends JPanel {
  private TitleBar titleBar = new TitleBar();
  private JComponent contentPane;
  private String captionText;
  private Icon captionIcon;

  public JTile() {
    this(null, null, null);
  }

  public JTile(JComponent contentPane, String caption) {
    this(contentPane, caption, null);
  }

  public JTile(String caption) {
    this(null, caption, null);
  }

  public JTile(JComponent contentPane) {
    this(contentPane, null, null);
  }

  public JTile(JComponent contentPane, String caption, Icon icon) {
    init();
    setContentPane(contentPane);
    setCaption(caption);
    setIcon(icon);
  }

  public void setContentPane(JComponent contentPane) {
    this.contentPane = contentPane;
    this.add(contentPane, BorderLayout.CENTER);
  }

  public JComponent getContentPane() {
    return contentPane;
  }

  public void setCaption(String text) {
    this.captionText = text;
    titleBar.setCaption(text);
  }

  public String getCaption() {
    return captionText;
  }

  public void setIcon(Icon icon) {
    this.captionIcon = icon;
    titleBar.setIcon(icon);
  }

  public Icon getIcon() {
    return captionIcon;
  }

  private void init() {
    this.setBorder(CustomBorderFactory.TILE_BORDER);
    this.setLayout(new BorderLayout());
    this.add(titleBar, BorderLayout.NORTH);
  }

  private class TitleBar extends JPanel {
    private Color leftColor;
    private JLabel caption = new JLabel(" ");
    private JLabel captionIcon = new JLabel(EmptyIcon.DEFAULT_ICON);

    private TitleBar() {
      leftColor = PlasticXPLookAndFeel.getSimpleInternalFrameBackground();
      caption.setOpaque(false);
      caption.setForeground(PlasticXPLookAndFeel.getSimpleInternalFrameForeground());
      caption.setBorder(Borders.DLU2_BORDER);
      captionIcon.setOpaque(false);
      captionIcon.setBorder(Borders.DLU2_BORDER);
      setLayout(new BorderLayout(0, 0));
      add(caption, BorderLayout.CENTER);
      add(captionIcon, BorderLayout.WEST);
    }

    private void setCaption(String text) {
      caption.setText((text != null && text.length() > 0 ? text : " "));
    }

    private void setIcon(Icon icon) {
      captionIcon.setIcon((icon != null ? icon : EmptyIcon.DEFAULT_ICON));
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D)g;
      Paint oldPaint = g2d.getPaint();
      int width = getWidth();
      int height = getHeight();
      Color highlight = UIManager.getColor("controlLtHighlight");
      Color shadow    = UIManager.getColor("controlShadow");

      g.setColor(highlight);
      g.drawLine(0, 0, width, 0);
      g.drawLine(0, 0, 0, height);
      g.setColor(shadow);
      g.drawLine(0, height-1, width, height-1);
      GradientPaint paint = new GradientPaint(
          1, height/2, leftColor, width, height/2, getBackground(), false);
      g2d.setPaint(paint);
      g2d.fillRect(1, 1, width-1, height-2);

      g2d.setPaint(oldPaint);
    }
  }
}

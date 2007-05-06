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

import java.awt.*;
import javax.swing.border.*;
import javax.swing.plaf.UIResource;
import javax.swing.UIManager;

/**
 *
 * @author Andriy Shapochka
 * @version 1.0
 */

public class CustomBorderFactory {
  public static final Border THIN_RAISED_BORDER = new ThinRaisedBorder();
  public static final Border THIN_LOWERED_BORDER = new ThinLoweredBorder();
  public static final Border TILE_BORDER = new TileBorder();

  public static class ThinRaisedBorder extends AbstractBorder {
    private static final Insets INSETS = new Insets(2, 2, 2, 2);

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
      g.translate(x, y);
      g.setColor(c.getBackground().brighter());
      g.drawLine(0, 0, w - 2, 0);
      g.drawLine(0, 0, 0, h - 2);
      g.setColor(c.getBackground().darker());
      g.drawLine(w - 1, 0, w - 1, h - 1);
      g.drawLine(0, h - 1, w - 1, h - 1);
      g.translate(-x, -y);
    }

    public Insets getBorderInsets(Component c) { return INSETS; }
  }


  public static class ThinLoweredBorder extends AbstractBorder {
    private static final Insets INSETS = new Insets(2, 2, 2, 2);

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
      g.translate(x, y);
      g.setColor(c.getBackground().darker());
      g.drawLine(0, 0, w - 2, 0);
      g.drawLine(0, 0, 0, h - 2);
      g.setColor(c.getBackground().brighter());
      g.drawLine(w - 1, 0, w - 1, h - 1);
      g.drawLine(0, h - 1, w - 1, h - 1);
      g.translate(-x, -y);
    }

    public Insets getBorderInsets(Component c) { return INSETS; }
  }

  public static class TileBorder extends AbstractBorder implements UIResource {

    public static final Insets INSETS	= new Insets(1, 1, 3, 3);

    static final int   ALPHA1			= 173;
    static final int   ALPHA2			=  66;


    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
      paintShadowedBorder(c, g, x, y, w, h);
    }

    private void paintShadowedBorder(Component c, Graphics g, int x, int y, int w, int h) {
      Color background	= c.getParent().getBackground();
      Color shadow    = UIManager.getColor("controlShadow");
      Color lightShadow   = new Color(shadow.getRed(),
                                      shadow.getGreen(),
                                      shadow.getBlue(),
                                      ALPHA1);
      Color lighterShadow = new Color(shadow.getRed(),
                                      shadow.getGreen(),
                                      shadow.getBlue(),
                                      ALPHA2);
      g.translate(x, y);
      // Dark border
      g.setColor(shadow);
      g.drawRect(0,   0, w-3, h-3);
      // Paint background before painting the shadow
      g.setColor(background);
      g.fillRect(w - 2, 0, 2, h);
      g.fillRect(0, h-2, w, 2);
      // Shadow line 1
      g.setColor(lightShadow);
      g.drawLine(w - 2, 1, w - 2, h - 2);
      g.drawLine(1, h - 2, w - 3, h - 2);
      // Shadow line2
      g.setColor(lighterShadow);
      g.drawLine(w - 1, 2, w - 1, h - 2);
      g.drawLine(2, h - 1, w - 2, h - 1);
      g.translate(-x, -y);
    }

    public Insets getBorderInsets(Component c) {
      return INSETS;
    }
  }
}

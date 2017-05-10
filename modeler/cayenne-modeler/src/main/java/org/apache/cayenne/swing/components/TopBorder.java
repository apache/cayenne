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

package org.apache.cayenne.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

/**
 * Line border only on top side of the component
 *
 * @since 4.0
 */
public class TopBorder extends AbstractBorder {

    private static final Color DEFAULT_COLOR = Color.LIGHT_GRAY;
    private static final Border DEFAULT_BORDER = new TopBorder(DEFAULT_COLOR, 1);

    private int thickness;
    private Color color;

    public static Border create() {
        return DEFAULT_BORDER;
    }

    public static Border create(int thickness) {
        return new TopBorder(DEFAULT_COLOR, thickness);
    }

    public static Border create(Color color) {
        return new TopBorder(color, 1);
    }

    public static Border create(Color color, int thickness) {
        return new TopBorder(color, thickness);
    }

    public TopBorder(Color color, int thickness) {
        this.color = color;
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if ((this.thickness > 0) && (g instanceof Graphics2D)) {
            Graphics2D g2d = (Graphics2D) g;

            Color oldColor = g2d.getColor();
            g2d.setColor(color);
            g2d.setBackground(color);

            g2d.drawRect(x, y, width, y + thickness - 1);

            g2d.setColor(oldColor);
        }
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(thickness, 0, 0, 0);
        return insets;
    }
}

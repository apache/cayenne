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

package org.apache.cayenne.swing.components.image;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;


/**
 * @since 4.0
 */
public class FilteredIconFactory {

    private static final JComponent DUMMY = new JPanel();

    public enum FilterType {
        DISABLE     (new DisabledFilter()),
        SELECTION   (new SelectionFilter()),
        WHITE       (new ColorFilter(0xFFFFFF)),
        GREEN       (new ColorFilter(0x65A91B)),
        VIOLET      (new ColorFilter(0xAD78DD)),
        BLUE        (new ColorFilter(0x53A3D6)),
        GRAY        (new ColorFilter(0x434343));

        private final RGBImageFilter filter;

        FilterType(RGBImageFilter filter) {
            this.filter = filter;
        }
    }

    static public Icon createDisabledIcon(Icon icon) {
        return createIcon(icon, FilterType.DISABLE);
    }

    static public Icon createIcon(Icon icon, FilterType filterType) {
        if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(DUMMY, img.getGraphics(), 0, 0);
            ImageProducer producer = new FilteredImageSource(img.getSource(), filterType.filter);
            Image resultImage = DUMMY.createImage(producer);
            return new ImageIcon(resultImage);
        }
        return null;
    }

    static class ColorFilter extends RGBImageFilter {

        private final int color;

        ColorFilter(int color) {
            canFilterIndexColorModel = true;
            this.color = color;
        }

        public int filterRGB(int x, int y, int argb) {
            int alpha = (argb >> 24) & 0xFF;
            return (alpha << 24) | color;
        }
    }

    static class SelectionFilter extends ColorFilter {
        SelectionFilter() {
            super(UIManager.getColor("Tree.selectionForeground").getRGB() & 0x00FFFFFF);
        }
    }

    static class DisabledFilter extends RGBImageFilter {

        private static final double COLOR_FACTOR = 0.4;
        private static final double ALPHA_FACTOR = 0.5;

        DisabledFilter() {
            canFilterIndexColorModel = true;
        }

        public int filterRGB(int x, int y, int rgb) {
            int a = (rgb >> 24) & 0xff;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >>  8) & 0xff;
            int b = (rgb      ) & 0xff;
            int luminance = (int)((1 - COLOR_FACTOR) * Math.min(255.0, (r + g + b) / 3.0));

            return  (int)(a * ALPHA_FACTOR) << 24 |
                    (int)(r * COLOR_FACTOR + luminance) << 16 |
                    (int)(g * COLOR_FACTOR + luminance) <<  8 |
                    (int)(b * COLOR_FACTOR + luminance);
        }
    }
}

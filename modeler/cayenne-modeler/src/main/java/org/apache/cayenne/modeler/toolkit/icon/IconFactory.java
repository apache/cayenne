/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.toolkit.icon;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.toolkit.ValueTypes;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.net.URL;
import java.util.Objects;


public class IconFactory {

    private static final String IMAGES_PATH = "org/apache/cayenne/modeler/images/";
    private static final JComponent DUMMY_PANEL = new JPanel();
    private static final RGBImageFilter DISABLED_FILTER = new DisabledFilter();

    private static final Icon domainIcon = IconFactory.buildIcon("icon-dom.png");
    private static final Icon nodeIcon = IconFactory.buildIcon("icon-node.png");
    private static final Icon mapIcon = IconFactory.buildIcon("icon-datamap.png");
    private static final Icon dbEntityIcon = IconFactory.buildIcon("icon-dbentity.png");
    private static final Icon objEntityIcon = IconFactory.buildIcon("icon-objentity.png");
    private static final Icon procedureIcon = IconFactory.buildIcon("icon-stored-procedure.png");
    private static final Icon queryIcon = IconFactory.buildIcon("icon-query.png");
    private static final Icon embeddableIcon = IconFactory.buildIcon("icon-embeddable.png");
    private static final Icon relationshipIcon = IconFactory.buildIcon("icon-relationship.png");
    private static final Icon attributeIcon = IconFactory.buildIcon("icon-attribute.png");

    public static Icon disabledIcon(Icon icon) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }

        BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
        icon.paintIcon(DUMMY_PANEL, img.getGraphics(), 0, 0);
        ImageProducer producer = new FilteredImageSource(img.getSource(), DISABLED_FILTER);
        Image resultImage = DUMMY_PANEL.createImage(producer);
        return new ImageIcon(resultImage);
    }

    /**
     * Creates an icon from an image file identified by the path, relative to  the modeler shared resources folder
     */
    public static ImageIcon buildIcon(String path) {
        URL url = ValueTypes.class.getClassLoader().getResource(IMAGES_PATH + path);
        Objects.requireNonNull(url);
        return new ImageIcon(url);
    }

    public static Icon iconForObject(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof DataChannelDescriptor) {
            return domainIcon;
        } else if (object instanceof DataNodeDescriptor) {
            return nodeIcon;
        } else if (object instanceof DataMap) {
            return mapIcon;
        } else if (object instanceof DbEntity) {
            return dbEntityIcon;
        } else if (object instanceof ObjEntity) {
            return objEntityIcon;
        } else if (object instanceof Procedure) {
            return procedureIcon;
        } else if (object instanceof QueryDescriptor) {
            return queryIcon;
        } else if (object instanceof Relationship) {
            return relationshipIcon;
        } else if (object instanceof Attribute) {
            return attributeIcon;
        } else if (object instanceof Embeddable) {
            return embeddableIcon;
        }
        return null;
    }

    private static class DisabledFilter extends RGBImageFilter {

        private static final double COLOR_FACTOR = 0.4;
        private static final double ALPHA_FACTOR = 0.5;

        DisabledFilter() {
            canFilterIndexColorModel = true;
        }

        public int filterRGB(int x, int y, int rgb) {
            int a = (rgb >> 24) & 0xff;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = (rgb) & 0xff;
            int luminance = (int) ((1 - COLOR_FACTOR) * Math.min(255.0, (r + g + b) / 3.0));

            return (int) (a * ALPHA_FACTOR) << 24 |
                    (int) (r * COLOR_FACTOR + luminance) << 16 |
                    (int) (g * COLOR_FACTOR + luminance) << 8 |
                    (int) (b * COLOR_FACTOR + luminance);
        }
    }
}

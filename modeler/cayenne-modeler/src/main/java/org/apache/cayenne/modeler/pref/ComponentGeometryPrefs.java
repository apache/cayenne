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

package org.apache.cayenne.modeler.pref;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.prefs.Preferences;

public class ComponentGeometryPrefs {

    private static final String HEIGHT_PROPERTY = "height";
    private static final String WIDTH_PROPERTY = "width";
    private static final String X_PROPERTY = "x";
    private static final String Y_PROPERTY = "y";

    public static void bindToTypePrefs(Component c, int defaultWidth, int defaultHeight) {
        Preferences prefs = Preferences.userNodeForPackage(c.getClass());
        bind(prefs, c, defaultWidth, defaultHeight);
    }

    private static void bind(Preferences prefs, Component c, int defaultWidth, int defaultHeight) {

        int w = prefs.getInt(WIDTH_PROPERTY, defaultWidth);
        int h = prefs.getInt(HEIGHT_PROPERTY, defaultHeight);

        if (w > 0 && h > 0) {
            c.setSize(w, h);
        }

        int x = prefs.getInt(X_PROPERTY, -1);
        int y = prefs.getInt(Y_PROPERTY, -1);

        if (x > 0 && y > 0) {
            c.setLocation(x, y);
        }

        c.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                prefs.putInt(WIDTH_PROPERTY, c.getWidth());
                prefs.putInt(HEIGHT_PROPERTY, c.getHeight());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                prefs.putInt(X_PROPERTY, c.getX());
                prefs.putInt(Y_PROPERTY, c.getY());
            }
        });
    }
}


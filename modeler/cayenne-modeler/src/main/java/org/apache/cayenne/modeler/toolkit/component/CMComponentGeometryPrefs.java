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

package org.apache.cayenne.modeler.toolkit.component;

import org.apache.cayenne.modeler.Application;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.prefs.Preferences;

public final class CMComponentGeometryPrefs {

    private static final String HEIGHT_PROPERTY = "height";
    private static final String WIDTH_PROPERTY = "width";
    private static final String X_PROPERTY = "x";
    private static final String Y_PROPERTY = "y";

    private final Preferences prefs;

    private CMComponentGeometryPrefs(Preferences prefs) {
        this.prefs = prefs;
    }

    public static CMComponentGeometryPrefs of(Class<?> anchor) {
        return new CMComponentGeometryPrefs(Application.getInstance().getPreferencesRepository().ui(anchor));
    }

    public static CMComponentGeometryPrefs of(Class<?> anchor, String path) {
        return new CMComponentGeometryPrefs(Application.getInstance().getPreferencesRepository().ui(anchor).node(path));
    }

    public void bind(Component c, int defaultWidth, int defaultHeight) {

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

        // Cache last value written to skip native Preferences writes when the
        // value hasn't changed. componentResized / componentMoved fire on every
        // pixel during a window drag.
        int[] last = { c.getWidth(), c.getHeight(), c.getX(), c.getY() };

        c.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                int width = c.getWidth();
                int height = c.getHeight();
                if (last[0] != width) {
                    prefs.putInt(WIDTH_PROPERTY, width);
                    last[0] = width;
                }
                if (last[1] != height) {
                    prefs.putInt(HEIGHT_PROPERTY, height);
                    last[1] = height;
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                int xPos = c.getX();
                int yPos = c.getY();
                if (last[2] != xPos) {
                    prefs.putInt(X_PROPERTY, xPos);
                    last[2] = xPos;
                }
                if (last[3] != yPos) {
                    prefs.putInt(Y_PROPERTY, yPos);
                    last[3] = yPos;
                }
            }
        });
    }
}

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

package org.apache.cayenne.modeler.toolkit.splitpane;

import javax.swing.*;
import java.util.prefs.Preferences;

public final class CMSplitPanePrefs {

    private final Preferences prefs;

    private CMSplitPanePrefs(Preferences prefs) {
        this.prefs = prefs;
    }

    public static CMSplitPanePrefs of(Class<?> anchor) {
        return new CMSplitPanePrefs(Preferences.userNodeForPackage(anchor));
    }

    public static CMSplitPanePrefs of(Class<?> anchor, String path) {
        return new CMSplitPanePrefs(Preferences.userNodeForPackage(anchor).node(path));
    }

    public void bind(JSplitPane p, int defaultLocation) {

        int dividerLocation = prefs.getInt(JSplitPane.DIVIDER_LOCATION_PROPERTY, defaultLocation);
        if (dividerLocation > 0) {
            p.setDividerLocation(dividerLocation);
        }

        p.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                e -> prefs.putInt(JSplitPane.DIVIDER_LOCATION_PROPERTY, p.getDividerLocation()));
    }
}

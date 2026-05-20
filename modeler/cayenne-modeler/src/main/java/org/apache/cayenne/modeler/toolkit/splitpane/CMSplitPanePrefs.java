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

import org.apache.cayenne.modeler.pref.PreferenceAdapter;
import org.apache.cayenne.modeler.pref.PrefsRepository;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public final class CMSplitPanePrefs extends PreferenceAdapter {

    private PropertyChangeListener listener;

    public CMSplitPanePrefs(PrefsRepository repository, String path) {
        super(repository.uiNode(path));
    }

    public void bind(JSplitPane pane, int defaultLocation) {
        unbind(pane);

        int dividerLocation = prefs.getInt(JSplitPane.DIVIDER_LOCATION_PROPERTY, defaultLocation);
        if (dividerLocation > 0) {
            pane.setDividerLocation(dividerLocation);
        }

        this.listener = e -> prefs.putInt(JSplitPane.DIVIDER_LOCATION_PROPERTY, pane.getDividerLocation());
        pane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, listener);
    }

    public void unbind(JSplitPane pane) {
        if (listener != null) {
            pane.removePropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, listener);
            listener = null;
        }
    }
}

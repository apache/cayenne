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

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.LastProjectsPreferences;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.event.model.RecentFileListListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Menu that contains a list of previously used files. It is built from CayenneModeler preferences via
 * {@link #rebuildFromPreferences()}
 */
public class RecentFileMenu extends JMenu implements RecentFileListListener {
    
    public RecentFileMenu(String s) {
        super(s);
    }

    /**
     * Rebuilds internal menu items list with the files stored in CayenneModeler preferences.
     */
    public void rebuildFromPreferences() {

        List<File> files = LastProjectsPreferences.getFiles();

        // read menus
        Component[] comps = getMenuComponents();
        int curSize = comps.length;
        int prefSize = files.size();

        OpenProjectAction action = Application.getInstance().getActionManager().getAction(OpenProjectAction.class);

        for (int i = 0; i < prefSize; i++) {
            String name = files.get(i).getAbsolutePath();
            if (i < curSize) {
                ((JMenuItem) comps[i]).setText(name);
            } else {

                JMenuItem item = new JMenuItem(name) {
                    @Override
                    protected void configurePropertiesFromAction(Action a) {
                        // exclude most generic action keys that are not applicable here
                        setIcon((Icon) a.getValue(Action.SMALL_ICON));
                        setEnabled(a.isEnabled());
                    }
                };

                item.setAction(action);
                add(item);
            }
        }

        // remove any hanging items
        for (int i = curSize - 1; i >= prefSize; i--) {
            remove(i);
        }
    }

    @Override
    public void recentFileListChanged() {
        rebuildFromPreferences();
        setEnabled(getMenuComponentCount() > 0);
    }
}

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

package org.apache.cayenne.modeler.util;

import java.awt.Component;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.event.RecentFileListListener;
import org.apache.cayenne.swing.control.FileMenuItem;

/**
 * Menu that contains a list of previously used files. It is built from CayenneModeler
 * preferences by calling <code>rebuildFromPreferences</code>.
 * 
 */
public class RecentFileMenu extends JMenu implements RecentFileListListener {
    /**
     * Constructor for RecentFileMenu.
     */
    public RecentFileMenu(String s) {
        super(s);
    }

    /**
     * @see javax.swing.JMenu#add(JMenuItem)
     */
    public FileMenuItem add(FileMenuItem menuItem) {
        return (FileMenuItem) super.add(menuItem);
    }

    /**
     * Rebuilds internal menu items list with the files stored in CayenneModeler
     * preferences.
     */
    public void rebuildFromPreferences() {
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        Vector<?> arr = pref.getVector(ModelerPreferences.LAST_PROJ_FILES);
        while (arr.size() > ModelerPreferences.LAST_PROJ_FILES_SIZE) {
            arr.remove(arr.size() - 1);
        }

        // read menus
        Component[] comps = getMenuComponents();
        int curSize = comps.length;
        int prefSize = arr.size();

        for (int i = 0; i < prefSize; i++) {
            String name = (String) arr.get(i);

            if (i < curSize) {
                // update existing one
                FileMenuItem item = (FileMenuItem) comps[i];
                item.setText(name);
            }
            else {
                // add a new one
                FileMenuItem item = new FileMenuItem(name);
                item.setAction(findAction());
                add(item);
            }
        }

        // remove any hanging items
        for (int i = curSize - 1; i >= prefSize; i--) {
            remove(i);
        }
    }

    protected Action findAction() {
        return Application.getInstance().getAction(OpenProjectAction.getActionName());
    }

    public void recentFileListChanged() {
        rebuildFromPreferences();
        setEnabled(getMenuComponentCount() > 0);
    }
}

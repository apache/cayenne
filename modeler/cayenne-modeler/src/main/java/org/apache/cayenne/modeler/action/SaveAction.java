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

package org.apache.cayenne.modeler.action;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.KeyStroke;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.pref.RenamedPreferences;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;

/**
 * An action that saves a project using to its default location.
 */
public class SaveAction extends SaveAsAction {

    public static String getActionName() {
        return "Save";
    }

    public SaveAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    @Override
    public String getIconName() {
        return "icon-save.png";
    }

    @Override
    protected boolean saveAll() throws Exception {
        Project p = getCurrentProject();
        if (p == null || p.getConfigurationResource() == null) {
            return super.saveAll();
        }

        String oldPath = p.getConfigurationResource().getURL().getPath();
        File oldProjectFile = new File(p.getConfigurationResource().getURL().toURI());

        getProjectController().getFileChangeTracker().pauseWatching();
        ProjectSaver saver = getApplication().getInjector().getInstance(ProjectSaver.class);
        saver.save(p);

        RenamedPreferences.removeOldPreferences();

        // if change DataChanelDescriptor name - as result change name of xml file
        // we will need change preferences path
        String[] path = oldPath.split("/");
        String[] newPath = p.getConfigurationResource().getURL().getPath().split("/");

        if (!path[path.length - 1].equals(newPath[newPath.length - 1])) {
            String newName = newPath[newPath.length - 1].replace(".xml", "");
            RenamedPreferences.copyPreferences(newName, getProjectController().getPreferenceForProject());
            RenamedPreferences.removeOldPreferences();
        }

        File newProjectFile = new File(p.getConfigurationResource().getURL().toURI());
        getApplication().getFrameController().changePathInLastProjListAction(oldProjectFile, newProjectFile);
        Application.getFrame().fireRecentFileListChanged();

        // Reset the watcher now
        getProjectController().getFileChangeTracker().reconfigure();

        return true;
    }
}

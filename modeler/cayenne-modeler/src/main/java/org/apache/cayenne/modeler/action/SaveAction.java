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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.model.ProjectSavedEvent;
import org.apache.cayenne.modeler.pref.RenamedPreferences;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * An action that saves a project using to its default location.
 */
public class SaveAction extends SaveAsAction {

    public SaveAction(Application application) {
        super("Save", application);
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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

        getProjectController().pauseFileChangeTracking();
        ProjectSaver saver = application.getProjectSaver();
        saver.save(p);

        RenamedPreferences.removeOldPreferences();

        // if change DataChanelDescriptor name - as result change name of xml file
        // we will need change preferences path
        String[] path = oldPath.split("/");
        String[] newPath = p.getConfigurationResource().getURL().getPath().split("/");

        if (!path[path.length - 1].equals(newPath[newPath.length - 1])) {
            String newName = newPath[newPath.length - 1].replace(".xml", "");
            RenamedPreferences.copyPreferences(newName, getProjectController().getPreferences());
            RenamedPreferences.removeOldPreferences();
        }

        File newProjectFile = new File(p.getConfigurationResource().getURL().toURI());
        application.getFrameController().changePathInLastProjListAction(oldProjectFile, newProjectFile);
        application.getFrameController().getView().fireRecentFileListChanged();

        getProjectController().fireProjectSavedEvent(new ProjectSavedEvent(getProjectController()));

        return true;
    }
}

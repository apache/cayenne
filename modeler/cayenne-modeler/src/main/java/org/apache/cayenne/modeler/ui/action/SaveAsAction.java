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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.model.ProjectBeforeSaveEvent;
import org.apache.cayenne.modeler.event.model.ProjectAfterSaveEvent;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * A "Save As" action that allows user to pick save location.
 */
public class SaveAsAction extends AppAction {

    private final ProjectOpener fileChooser;

    public SaveAsAction(Application application) {
        this("Save As...", application);
    }

    protected SaveAsAction(String name, Application application) {
        super(application, name);
        this.fileChooser = new ProjectOpener();
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
    }

    /**
     * Saves project and related files. Saving is done to temporary files, and
     * only on successful save, master files are replaced with new versions.
     */
    protected boolean saveAll() throws Exception {
        Project p = getCurrentProject();

        File projectDir = fileChooser.newProjectDir(app, p);
        if (projectDir == null) {
            return false;
        }

        if (projectDir.exists() && !projectDir.canWrite()) {
            JOptionPane.showMessageDialog(app.getFrame(), "Can't save project - unable to write to file \""
                    + projectDir.getPath() + "\"", "Can't Save Project", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        getProjectSession().pauseFileChangeTracking();

        PreferencesRepository repo = app.getPreferencesRepository();
        repo.stageProjectMove(p, projectDir);
        DataChannelDescriptor descriptor = (DataChannelDescriptor) p.getRootNode();
        if (descriptor != null) {
            for (DataMap map : descriptor.getDataMaps()) {
                repo.stageDataMapMove(map, projectDir);
            }
        }

        URLResource res = new URLResource(projectDir.toURI().toURL());
        ProjectSaver saver = app.getProjectSaver();
        saver.saveAs(p, res);

        File file = new File(p.getConfigurationResource().getURL().toURI());
        app.getFrame().addToLastProjListAction(file);
        app.getFrame().fireRecentFileListChanged();

        getProjectSession().fireProjectAfterSaveEvent(new ProjectAfterSaveEvent(getProjectSession()));

        return true;
    }

    /**
     * This method is synchronized to prevent problems on double-clicking
     * "save".
     */
    @Override
    public void performAction(ActionEvent e) {
        performAction();
    }

    public void performAction() {

        ProjectValidator projectValidator = app.getProjectValidator();
        ValidationResult validationResult = projectValidator.validate(getCurrentProject().getRootNode());

        getProjectSession().fireProjectBeforeSaveEvent(new ProjectBeforeSaveEvent(SaveAsAction.class));

        try {
            if (!saveAll()) {
                return;
            }
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error on save", ex);
        }

        app.getFrame().onProjectSaved();

        // If there were errors or warnings at validation, display them
        if (!validationResult.getFailures().isEmpty()) {
            app.getActionManager().getAction(ValidateAction.class)
                    .showFailures(validationResult.getFailures());
        }
    }

    /**
     * Returns <code>true</code> if path contains a Project object and the
     * project is modified.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        Project project = app.getFrame().getProjectSession().project();
        return project != null && project.isModified();
    }
}

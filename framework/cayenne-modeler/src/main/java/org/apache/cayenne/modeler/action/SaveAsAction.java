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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.validator.ValidationDisplayHandler;
import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.project.validator.ValidationInfo;
import org.apache.cayenne.project2.Project;
import org.apache.cayenne.project2.ProjectSaver;
import org.apache.cayenne.project2.validate.ConfigurationValidationVisitor;
import org.apache.cayenne.resource.URLResource;

/**
 * A "Save As" action that allows user to pick save location.
 * 
 */
public class SaveAsAction extends CayenneAction {

    protected ProjectOpener fileChooser;

    public static String getActionName() {
        return "Save As...";
    }

    public SaveAsAction(Application application) {
        this(getActionName(), application);
    }

    protected SaveAsAction(String name, Application application) {
        super(name, application);
        this.fileChooser = new ProjectOpener();
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask()
                | ActionEvent.SHIFT_MASK);
    }

    /**
     * Saves project and related files. Saving is done to temporary files, and only on
     * successful save, master files are replaced with new versions.
     */
    protected boolean saveAll() throws Exception {
        Project p = getCurrentProject();

        // obtain preference object before save, when the project path may change.....
        Domain preference = getProjectController().getPreferenceDomainForProject();

        File projectDir = fileChooser.newProjectDir(Application.getFrame(), p);
        if (projectDir == null) {
            return false;
        }
        
        if (projectDir.exists() && !projectDir.canWrite()) {
            JOptionPane.showMessageDialog(Application.getFrame(),
                    "Can't save project - unable to write to file \"" + projectDir.getPath() + "\"",
                    "Can't Save Project", JOptionPane.OK_OPTION);
            return false;
        }
        
        getProjectController().getProjectWatcher().pauseWatching();
        
        URL url = projectDir.toURL();
        
        URLResource res = new URLResource(url);
        ///!!!!!!!!!!!!!!!!!!! SAVE AS!!!!!!!!!!!!!!
        ProjectSaver saver = getApplication().getInjector().getInstance(ProjectSaver.class);
        saver.saveAs(p, res);

        // update preferences domain key
        preference.rename(projectDir.getPath());
        
        getApplication().getFrameController().addToLastProjListAction(
                p.getConfigurationResource().getURL().getPath());
        Application.getFrame().fireRecentFileListChanged();

        /**
         * Reset the watcher now
         */
        getProjectController().getProjectWatcher().reconfigure();

        return true;
    }

    /**
     * This method is synchronized to prevent problems on double-clicking "save".
     */
    public synchronized void performAction(ActionEvent e) {
        performAction(ValidationDisplayHandler.WARNING);
    }

    public synchronized void performAction(int warningLevel) {
        
        ConfigurationValidationVisitor validatVisitor = new ConfigurationValidationVisitor(getCurrentProject());
        List<ValidationInfo> object = (List<ValidationInfo>) getCurrentProject().getRootNode().acceptVisitor(validatVisitor);
        int validationCode = validatVisitor.getMaxSeverity();

        // If no serious errors, perform save.
        if (validationCode < ValidationDisplayHandler.ERROR) {
            try {
                if (!saveAll()) {
                    return;
                }
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error on save", ex);
            }

            getApplication().getFrameController().projectSavedAction();
        }

        // If there were errors or warnings at validation, display them
        if (validationCode >= warningLevel) {
            ValidatorDialog.showDialog(Application.getFrame(), object);
        }
    }

    /**
     * Returns <code>true</code> if path contains a Project object and the project is
     * modified.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        Project project = path.firstInstanceOf(Project.class);
        return project != null && project.isModified();
    }
}

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

package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.mvc.RootController;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.pref.LastProjectsPreferences;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportController;
import org.apache.cayenne.modeler.ui.project.validator.ProjectValidatorDialogController;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Controller of the main application frame.
 */
public class ModelerController extends RootController {

    private final ProjectController projectController;
    private final ModelerFrame view;
    private final DbImportController dbImportController;

    public ModelerController(Application application) {
        super(application);

        this.view = new ModelerFrame(application.getActionManager(), application.getLogConsoleController());
        application.getPlatformInitializer().setupMenus(view);
        this.projectController = new ProjectController(this);
        this.dbImportController = new DbImportController();
    }

    @Override
    public ModelerFrame getView() {
        return view;
    }

    public ProjectController getProjectController() {
        return projectController;
    }

    public FSPath getLastEOModelDirectory() {
        // find start directory in preferences

        FSPath path = (FSPath) application
                .getCayenneProjectPreferences()
                .getProjectDetailObject(FSPath.class, getViewPreferences().node("lastEOMDir"));

        if (path.getPath() == null) {
            path.setPath(getLastDirectory().getPath());
        }

        return path;
    }

    private boolean processDropAction(Transferable transferable) {
        List<File> fileList;
        try {
            fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception e) {
            return false;
        }

        File transferFile = fileList.get(0);
        if (transferFile.isFile()) {
            FileFilter filter = FileFilters.getApplicationFilter();
            if (filter.accept(transferFile)) {
                Application.getInstance().getActionManager().getAction(OpenProjectAction.class).openProject(transferFile);
                return true;
            }
        }

        return false;
    }

    public void onStartup() {
        view.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                projectController.saveSelectionToPrefs();
                getApplication().getActionManager().getAction(ExitAction.class).exit();
            }
        });

        // Register a hook to save the window position when quit via the app menu.
        // This is in Mac OSX only.
        if (System.getProperty("os.name").startsWith("Mac OS")) {
            Runnable runner = projectController::saveSelectionToPrefs;
            Runtime.getRuntime().addShutdownHook(new Thread(runner, "Window Prefs Hook"));
        }

        new DropTarget(view, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(dtde.getDropAction());
                Transferable transferable = dtde.getTransferable();
                dtde.dropComplete(processDropAction(transferable));
            }
        });

        new ComponentGeometry(view.getClass(), null).resetAndTrackGeometry(view, 1200, 720, 0);

        view.setVisible(true);
    }

    public void onProjectModified() {
        view.setTitle("* - " + getProjectLocationString());
    }

    public void onProjectSaved() {
        projectController.setDirty(false);
        updateStatus("Project saved...");
        view.setTitle(getProjectLocationString());
    }

    public void onProjectClosed() {
        projectController.saveSelectionToPrefs();

        // --- update view
        view.setEditorPanel(null);

        // repaint is needed, since sometimes there is a
        // trace from menu left on the screen
        view.repaint();
        view.setTitle("");

        projectController.projectClosed();
        application.getActionManager().projectClosed();

        updateStatus("Project Closed...");
    }

    /**
     * Handles project opening control. Updates main frame, then delegates control to child controllers.
     */
    public void onProjectOpened(Project project) {

        projectController.projectOpened(project);
        view.setTitle(getProjectLocationString());
        view.setEditorPanel(projectController.getView());

        projectController.restoreSelectionFromPrefs();
        application.getActionManager().projectOpened();

        // do status update AFTER the project is actually opened...
        if (project.getConfigurationResource() == null) {
            updateStatus("New project created...");
        } else {
            updateStatus("Project opened...");
            try {
                // update preferences
                File file = new File(project.getConfigurationResource().getURL().toURI());
                getLastDirectory().setDirectory(file);
                view.fireRecentFileListChanged();
            } catch (URISyntaxException ignore) {
            }
        }

        // for validation purposes combine load failures with post-load validation (not
        // sure if that'll cause duplicate messages?).
        List<ValidationFailure> allFailures = new ArrayList<>();
        Collection<ValidationFailure> loadFailures = project.getConfigurationTree().getLoadFailures();

        if (!loadFailures.isEmpty()) {
            // mark project as unsaved
            project.setModified(true);
            projectController.setDirty(true);
            allFailures.addAll(loadFailures);
        }

        ProjectValidator projectValidator = getApplication().getProjectValidator();
        ValidationResult validationResult = projectValidator.validate(project.getRootNode());
        allFailures.addAll(validationResult.getFailures());

        if (!allFailures.isEmpty()) {
            new ProjectValidatorDialogController(projectController).showOnFailures(validationResult.getFailures());
        }
    }

    /**
     * Adds path to the list of last opened projects in preferences.
     */
    public void addToLastProjListAction(File file) {
        Preferences prefs = LastProjectsPreferences.getPrefs();
        List<File> files = LastProjectsPreferences.getFiles();

        files.remove(file);
        files.add(0, file);

        List<File> truncatedFiles = files.stream()
                .limit(LastProjectsPreferences.LAST_PROJ_FILES_SIZE)
                .collect(Collectors.toList());

        try {
            prefs.clear();
        } catch (BackingStoreException ignored) {
            // ignore exception
        }

        int size = truncatedFiles.size();
        for (int i = 0; i < size; i++) {
            prefs.put(String.valueOf(i), truncatedFiles.get(i).getAbsolutePath());
        }
    }

    public void changePathInLastProjListAction(File oldFile, File newFile) {
        LastProjectsPreferences.getFiles().remove(oldFile);

        addToLastProjListAction(newFile);

        getLastDirectory().setDirectory(newFile);
        view.fireRecentFileListChanged();
    }

    /**
     * Performs status bar update with a message. Message will disappear in 6 seconds.
     */
    public void updateStatus(String message) {
        view.getStatus().setText(message);

        // start message cleanup thread that would remove the message after X seconds
        if (message != null && !message.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    Thread.sleep(6 * 10000);
                } catch (InterruptedException ignore) {
                }
                if (message.equals(view.getStatus().getText())) {
                    updateStatus(null);
                }
            }).start();
        }
    }

    public DbImportController getDbImportController() {
        return dbImportController;
    }

    protected String getProjectLocationString() {
        if (projectController.getProject().getConfigurationResource() == null) {
            return "[New Project]";
        }
        try {
            File projectFile = new File(projectController.getProject().getConfigurationResource().getURL().toURI());
            return projectFile.toString();
        } catch (URISyntaxException e) {
            throw new CayenneRuntimeException("Invalid project source URL", e);
        }
    }
}

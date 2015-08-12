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

package org.apache.cayenne.modeler;

import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.init.platform.PlatformInitializer;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.modeler.util.state.ProjectStateUtil;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Controller of the main application frame.
 */
public class CayenneModelerController extends CayenneController {

    private static final ProjectStateUtil PROJECT_STATE_UTIL = new ProjectStateUtil();

    protected ProjectController projectController;

    protected CayenneModelerFrame frame;
	private EditorView editorView;

    public CayenneModelerController(){}

    public CayenneModelerController(Application application) {
        super(application);

        this.frame = new CayenneModelerFrame(application.getActionManager());

        application
                .getInjector()
                .getInstance(PlatformInitializer.class)
                .setupMenus(frame);

        this.projectController = new ProjectController(this);
    }

    public Component getView() {
        return frame;
    }

    public ProjectController getProjectController() {
        return projectController;
    }

    public FSPath getLastEOModelDirectory() {
        // find start directory in preferences

        FSPath path = (FSPath) application
                .getCayenneProjectPreferences()
                .getProjectDetailObject(
                        FSPath.class,
                        getViewPreferences().node("lastEOMDir"));

        if (path.getPath() == null) {
            path.setPath(getLastDirectory().getPath());
        }

        return path;
    }

    protected void initBindings() {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                PROJECT_STATE_UTIL.saveLastState(projectController);
                getApplication().getActionManager().getAction(ExitAction.class).exit();
            }
        });

        // Register a hook to save the window position when quit via the app menu.
        // This is in Mac OSX only.
        if (System.getProperty("os.name").startsWith("Mac OS")) {
            Runnable runner = new Runnable() {
                @Override
                public void run() {
                    PROJECT_STATE_UTIL.saveLastState(projectController);
                }
            };

            Runtime.getRuntime().addShutdownHook(new Thread(runner, "Window Prefs Hook"));
        }

        new DropTarget(frame, new DropTargetAdapter() {

            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(dtde.getDropAction());
                Transferable transferable = dtde.getTransferable();
                dtde.dropComplete(processDropAction(transferable));
            }
        });

        ComponentGeometry geometry = new ComponentGeometry(frame.getClass(), null);
        geometry.bind(frame, 650, 550, 0);
    }

    private boolean processDropAction(Transferable transferable) {
        List<File> fileList;
        try {
            fileList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (Exception e) {
            return false;
        }

        if (fileList != null) {

        File transferFile = fileList.get(0);
            if (transferFile.isFile()) {

                FileFilter filter = FileFilters.getApplicationFilter();

                if (filter.accept(transferFile)) {
                    ActionEvent e = new ActionEvent(
                            transferFile,
                            ActionEvent.ACTION_PERFORMED,
                            "OpenProject");
                    Application.getInstance().getActionManager().getAction(
                            OpenProjectAction.class).actionPerformed(e);
                    return true;
                }
            }
        }

        return false;
    }

    public void startupAction() {
        initBindings();
        frame.setVisible(true);
    }

    public void projectModifiedAction() {
        String title = (projectController.getProject().getConfigurationResource() == null)
                ? "[New]"
                : projectController
                        .getProject()
                        .getConfigurationResource()
                        .getURL()
                        .getPath();

        frame.setTitle("* - " + ModelerConstants.TITLE + " - " + title);
    }

    public void projectSavedAction() {
        projectController.setDirty(false);
        projectController.updateProjectControllerPreferences();
        updateStatus("Project saved...");
        frame.setTitle(ModelerConstants.TITLE
                + " - "
                + projectController
                        .getProject()
                        .getConfigurationResource()
                        .getURL()
                        .getPath());
    }

    /**
     * Action method invoked on project closing.
     */
    public void projectClosedAction() {
        PROJECT_STATE_UTIL.saveLastState(projectController);

        // --- update view
        frame.setView(null);

        // repaint is needed, since sometimes there is a
        // trace from menu left on the screen
        frame.repaint();
        frame.setTitle(ModelerConstants.TITLE);

        projectController.setProject(null);

        projectController.reset();
        application.getActionManager().projectClosed();

        updateStatus("Project Closed...");
    }

    /**
     * Handles project opening control. Updates main frame, then delegates control to
     * child controllers.
     */
    public void projectOpenedAction(Project project) {

        projectController.setProject(project);

        editorView = new EditorView(projectController);
        frame.setView(editorView);

        projectController.projectOpened();
        application.getActionManager().projectOpened();

        // do status update AFTER the project is actually opened...
        if (project.getConfigurationResource() == null) {
            updateStatus("New project created...");
            frame.setTitle(ModelerConstants.TITLE + "- [New]");
        }
        else {
            updateStatus("Project opened...");
            frame.setTitle(ModelerConstants.TITLE
                    + " - "
                    + project.getConfigurationResource().getURL().getPath());
        }

        // update preferences
        if (project.getConfigurationResource() != null) {
            getLastDirectory().setDirectory(
                    new File(project.getConfigurationResource().getURL().getPath()));
            frame.fireRecentFileListChanged();
        }

        PROJECT_STATE_UTIL.fireLastState(projectController);

        // for validation purposes combine load failures with post-load validation (not
        // sure if that'll cause duplicate messages?).
        List<ValidationFailure> allFailures = new ArrayList<ValidationFailure>();
        Collection<ValidationFailure> loadFailures = project
                .getConfigurationTree()
                .getLoadFailures();

        if (!loadFailures.isEmpty()) {
            // mark project as unsaved
            project.setModified(true);
            projectController.setDirty(true);
            allFailures.addAll(loadFailures);
        }

        ProjectValidator projectValidator = getApplication().getInjector().getInstance(
                ProjectValidator.class);
        ValidationResult validationResult = projectValidator.validate(project
                .getRootNode());
        allFailures.addAll(validationResult.getFailures());

        if (!allFailures.isEmpty()) {
            ValidatorDialog.showDialog(frame, validationResult.getFailures());
        }
    }

    public EditorView getEditorView() {
    	return editorView;
    }

	/** Adds path to the list of last opened projects in preferences. */
    public void addToLastProjListAction(String path) {

        Preferences frefLastProjFiles = ModelerPreferences.getLastProjFilesPref();
        List<String> arr = ModelerPreferences.getLastProjFiles();
        // Add proj path to the preferences
        // Prevent duplicate entries.
        if (arr.contains(path)) {
            arr.remove(path);
        }

        arr.add(0, path);
        while (arr.size() > ModelerPreferences.LAST_PROJ_FILES_SIZE) {
            arr.remove(arr.size() - 1);
        }

        try {
            frefLastProjFiles.clear();
        }
        catch (BackingStoreException e) {
            // ignore exception
        }
        int size = arr.size();

        for (int i = 0; i < size; i++) {
            frefLastProjFiles.put(String.valueOf(i), arr.get(i).toString());
        }
    }

    /**
     * Performs status bar update with a message. Message will dissappear in 6 seconds.
     */
    public void updateStatus(String message) {
        frame.getStatus().setText(message);

        // start message cleanup thread that would remove the message after X seconds
        if (message != null && message.trim().length() > 0) {
            Thread cleanup = new ExpireThread(message, 6);
            cleanup.start();
        }
    }

    class ExpireThread extends Thread {

        protected int seconds;
        protected String message;

        public ExpireThread(String message, int seconds) {
            this.seconds = seconds;
            this.message = message;
        }

        public void run() {
            try {
                sleep(seconds * 1000);
            }
            catch (InterruptedException e) {
                // ignore exception
            }

            if (message.equals(frame.getStatus().getText())) {
                updateStatus(null);
            }
        }
    }

    public void changePathInLastProjListAction(String oldPath, String newPath) {
        Preferences frefLastProjFiles = ModelerPreferences.getLastProjFilesPref();
        List<String> arr = ModelerPreferences.getLastProjFiles();
        // Add proj path to the preferences
        // Prevent duplicate entries.
        if (arr.contains(oldPath)) {
            arr.remove(oldPath);
        }

        if (arr.contains(newPath)) {
            arr.remove(newPath);
        }

        arr.add(0, newPath);
        while (arr.size() > ModelerPreferences.LAST_PROJ_FILES_SIZE) {
            arr.remove(arr.size() - 1);
        }

        try {
            frefLastProjFiles.clear();
        }
        catch (BackingStoreException e) {
            // ignore exception
        }
        int size = arr.size();

        for (int i = 0; i < size; i++) {
            frefLastProjFiles.put(String.valueOf(i), arr.get(i).toString());
        }

        getLastDirectory().setDirectory(new File(newPath));
        frame.fireRecentFileListChanged();
    }

	
}

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

package org.apache.cayenne.modeler;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.editor.DbImportController;
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
import java.net.URISyntaxException;
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

    private ProjectController projectController;

    protected CayenneModelerFrame frame;
	private EditorView editorView;

	private DbImportController dbImportController;

    public CayenneModelerController(){
    }

    public CayenneModelerController(Application application) {
        super(application);

        this.frame = new CayenneModelerFrame(application.getActionManager());
        application.getInjector().getInstance(PlatformInitializer.class).setupMenus(frame);
        this.projectController = new ProjectController(this);
        this.dbImportController = new DbImportController();
    }

    @Override
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
                .getProjectDetailObject(FSPath.class, getViewPreferences().node("lastEOMDir"));

        if (path.getPath() == null) {
            path.setPath(getLastDirectory().getPath());
        }

        return path;
    }

    protected void initBindings() {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                PROJECT_STATE_UTIL.saveLastState(projectController);
                getApplication().getActionManager().getAction(ExitAction.class).exit();
            }
        });

        // Register a hook to save the window position when quit via the app menu.
        // This is in Mac OSX only.
        if (System.getProperty("os.name").startsWith("Mac OS")) {
            Runnable runner = () -> PROJECT_STATE_UTIL.saveLastState(projectController);
            Runtime.getRuntime().addShutdownHook(new Thread(runner, "Window Prefs Hook"));
        }

        new DropTarget(frame, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(dtde.getDropAction());
                Transferable transferable = dtde.getTransferable();
                dtde.dropComplete(processDropAction(transferable));
            }
        });

        ComponentGeometry geometry = new ComponentGeometry(frame.getClass(), null);
        geometry.bind(frame, 1200, 720, 0);
    }


    @SuppressWarnings("unchecked")
    private boolean processDropAction(Transferable transferable) {
        List<File> fileList;
        try {
            fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception e) {
            return false;
        }

        if (fileList != null) {
            File transferFile = fileList.get(0);
            if (transferFile.isFile()) {
                FileFilter filter = FileFilters.getApplicationFilter();
                if (filter.accept(transferFile)) {
                    ActionEvent e = new ActionEvent(transferFile, ActionEvent.ACTION_PERFORMED, "OpenProject");
                    Application.getInstance().getActionManager().getAction(OpenProjectAction.class).actionPerformed(e);
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
        frame.setTitle("* - " + getProjectLocationString());
    }

    public void projectSavedAction() {
        projectController.setDirty(false);
        projectController.updateProjectControllerPreferences();
        updateStatus("Project saved...");
        frame.setTitle(getProjectLocationString());
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
        frame.setTitle("");

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
        } else {
            updateStatus("Project opened...");
            try {
                // update preferences
                File file = new File(project.getConfigurationResource().getURL().toURI());
                getLastDirectory().setDirectory(file);
                frame.fireRecentFileListChanged();
            } catch (URISyntaxException ignore) {
            }
        }

        frame.setTitle(getProjectLocationString());

        PROJECT_STATE_UTIL.fireLastState(projectController);

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

        ProjectValidator projectValidator = getApplication().getInjector().getInstance(ProjectValidator.class);
        ValidationResult validationResult = projectValidator.validate(project.getRootNode());
        allFailures.addAll(validationResult.getFailures());

        if (!allFailures.isEmpty()) {
            ValidatorDialog.showDialog(frame, validationResult.getFailures());
        }
    }

    public EditorView getEditorView() {
    	return editorView;
    }

	/** Adds path to the list of last opened projects in preferences. */
    public void addToLastProjListAction(File file) {
        Preferences prefLastProjFiles = ModelerPreferences.getLastProjFilesPref();
        List<File> arr = ModelerPreferences.getLastProjFiles();

        // Add proj path to the preferences
        arr.remove(file);
        arr.add(0, file);
        while (arr.size() > ModelerPreferences.LAST_PROJ_FILES_SIZE) {
            arr.remove(arr.size() - 1);
        }

        try {
            prefLastProjFiles.clear();
        } catch (BackingStoreException ignored) {
            // ignore exception
        }

        int size = arr.size();
        for (int i = 0; i < size; i++) {
            prefLastProjFiles.put(String.valueOf(i), arr.get(i).getAbsolutePath());
        }
    }

    public void changePathInLastProjListAction(File oldFile, File newFile) {
        ModelerPreferences.getLastProjFiles().remove(oldFile);

        addToLastProjListAction(newFile);

        getLastDirectory().setDirectory(newFile);
        frame.fireRecentFileListChanged();
    }

    /**
     * Performs status bar update with a message. Message will disappear in 6 seconds.
     */
    public void updateStatus(String message) {
        frame.getStatus().setText(message);

        // start message cleanup thread that would remove the message after X seconds
        if (message != null && message.trim().length() > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(6 * 10000);
                } catch (InterruptedException ignore){
                }
                if (message.equals(frame.getStatus().getText())) {
                    updateStatus(null);
                }
            }).start();
        }
    }

    public DbImportController getDbImportController() {
        return dbImportController;
    }

    protected String getProjectLocationString() {
        if(projectController.getProject().getConfigurationResource() == null) {
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

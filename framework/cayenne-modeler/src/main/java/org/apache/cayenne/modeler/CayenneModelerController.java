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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.WindowConstants;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.validator.Validator;

/**
 * Controller of the main application frame.
 * 
 */
public class CayenneModelerController extends CayenneController {

    protected ProjectController projectController;

    protected CayenneModelerFrame frame;
    protected File initialProject;

    public CayenneModelerController(Application application, File initialProject) {
        super(application);

        this.initialProject = initialProject;
        this.frame = new CayenneModelerFrame(application.getActionManager());
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

        FSPath path = (FSPath) getViewDomain()
                .getDetail("lastEOMDir", FSPath.class, true);

        if (path.getPath() == null) {
            path.setPath(getLastDirectory().getPath());
        }

        return path;
    }

    protected void initBindings() {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                ((ExitAction) getApplication().getAction(ExitAction.getActionName()))
                        .exit();
            }
        });

        new DropTarget(frame, new DropTargetAdapter() {

            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(dtde.getDropAction());
                Transferable transferable = dtde.getTransferable();
                dtde.dropComplete(processDropAction(transferable));
            }
        });

        Domain prefDomain = application.getPreferenceDomain().getSubdomain(
                frame.getClass());
        ComponentGeometry geometry = ComponentGeometry.getPreference(prefDomain);
        geometry.bind(frame, 650, 550, 30);
    }

    private boolean processDropAction(Transferable transferable) {
        List fileList;
        try {
            fileList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (Exception e) {
            return false;
        }

        File transferFile = (File) fileList.get(0);

        if (transferFile.isFile()) {

            if (Configuration.DEFAULT_DOMAIN_FILE.equals(transferFile.getName())) {
                ActionEvent e = new ActionEvent(
                        transferFile,
                        ActionEvent.ACTION_PERFORMED,
                        "OpenProject");
                Application
                        .getInstance()
                        .getAction(OpenProjectAction.getActionName())
                        .actionPerformed(e);
                return true;
            }
        }

        return false;
    }

    public void startupAction() {
        initBindings();
        frame.setVisible(true);

        // open project
        if (initialProject != null) {
            OpenProjectAction openAction = (OpenProjectAction) getApplication()
                    .getAction(OpenProjectAction.getActionName());
            openAction.openProject(initialProject);
        }
    }

    public void projectModifiedAction() {
        String title = (projectController.getProject().isLocationUndefined())
                ? "[New]"
                : projectController.getProject().getMainFile().getAbsolutePath();

        frame.setTitle("* - " + ModelerConstants.TITLE + " - " + title);
    }

    public void projectSavedAction() {
        projectController.setDirty(false);
        updateStatus("Project saved...");
        frame.setTitle(ModelerConstants.TITLE
                + " - "
                + projectController.getProject().getMainFile().getAbsolutePath());
    }

    /**
     * Action method invoked on project closing.
     */
    public void projectClosedAction() {
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

        frame.setView(new EditorView(projectController));

        projectController.projectOpened();
        application.getActionManager().projectOpened();

        // do status update AFTER the project is actually opened...
        if (project.isLocationUndefined()) {
            updateStatus("New project created...");
            frame.setTitle(ModelerConstants.TITLE + "- [New]");
        }
        else {
            updateStatus("Project opened...");
            frame.setTitle(ModelerConstants.TITLE
                    + " - "
                    + project.getMainFile().getAbsolutePath());
        }

        // update preferences
        if (!project.isLocationUndefined()) {
            getLastDirectory().setDirectory(project.getProjectDirectory());
            frame.fireRecentFileListChanged();
        }

        // --- check for load errors
        if (project.getLoadStatus().hasFailures()) {
            // mark project as unsaved
            project.setModified(true);
            projectController.setDirty(true);

            // show warning dialog
            ValidatorDialog.showDialog(frame, new Validator(project, project
                    .getLoadStatus()));
        }

    }

    /** Adds path to the list of last opened projects in preferences. */
    public void addToLastProjListAction(String path) {
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        Vector arr = pref.getVector(ModelerPreferences.LAST_PROJ_FILES);
        // Add proj path to the preferences
        // Prevent duplicate entries.
        if (arr.contains(path)) {
            arr.remove(path);
        }

        arr.insertElementAt(path, 0);
        while (arr.size() > ModelerPreferences.LAST_PROJ_FILES_SIZE) {
            arr.remove(arr.size() - 1);
        }

        pref.remove(ModelerPreferences.LAST_PROJ_FILES);
        Iterator iter = arr.iterator();
        while (iter.hasNext()) {
            pref.addProperty(ModelerPreferences.LAST_PROJ_FILES, iter.next());
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
}

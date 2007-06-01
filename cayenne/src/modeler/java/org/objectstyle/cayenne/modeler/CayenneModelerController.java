/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JFrame;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.modeler.action.ExitAction;
import org.objectstyle.cayenne.modeler.action.OpenProjectAction;
import org.objectstyle.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.objectstyle.cayenne.modeler.editor.EditorView;
import org.objectstyle.cayenne.modeler.pref.ComponentGeometry;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.modeler.util.RecentFileMenu;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.validator.Validator;

/**
 * Controller of the main application frame.
 * 
 * @author Andrei Adamchik
 */
public class CayenneModelerController extends CayenneController {

    protected ProjectController projectController;
    protected ActionController actionController;

    protected CayenneModelerFrame frame;
    protected File initialProject;

    public CayenneModelerController(Application application, File initialProject) {
        super(application);

        this.initialProject = initialProject;
        this.frame = new CayenneModelerFrame(this);

        projectController = new ProjectController(this);
        actionController = new ActionController(application);
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
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                ((ExitAction) getApplication().getAction(ExitAction.getActionName()))
                        .exit();
            }
        });

        Domain prefDomain = application.getPreferenceDomain().getSubdomain(
                frame.getClass());
        ComponentGeometry geometry = ComponentGeometry.getPreference(prefDomain);
        geometry.bind(frame, 650, 550, 30);
    }

    public void dataDomainSelectedAction(DataDomain domain) {
        actionController.domainSelected(domain);
    }

    public void startupAction() {
        initBindings();
        frame.show();

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
        RecentFileMenu recentFileMenu = frame.getRecentFileMenu();
        recentFileMenu.rebuildFromPreferences();
        recentFileMenu.setEnabled(recentFileMenu.getMenuComponentCount() > 0);

        frame.setView(null);

        // repaint is needed, since sometimes there is a
        // trace from menu left on the screen
        frame.repaint();
        frame.setTitle(ModelerConstants.TITLE);

        projectController.setProject(null);

        projectController.reset();
        actionController.projectClosed();

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
        actionController.projectOpened();

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
        }

        // --- check for load errors
        if (project.getLoadStatus().hasFailures()) {
            // mark project as unsaved
            project.setModified(true);
            projectController.setDirty(true);

            // show warning dialog
            ValidatorDialog.showDialog(frame, projectController, new Validator(
                    project,
                    project.getLoadStatus()));
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
        while (arr.size() > 4) {
            arr.remove(arr.size() - 1);
        }

        pref.remove(ModelerPreferences.LAST_PROJ_FILES);
        Iterator iter = arr.iterator();
        while (iter.hasNext()) {
            pref.addProperty(ModelerPreferences.LAST_PROJ_FILES, iter.next());
        }
    }

    /**
     * Returns the child action controller.
     */
    public ActionController getActionController() {
        return actionController;
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
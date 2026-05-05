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
import org.apache.cayenne.modeler.pref.RecentProjectsPrefs;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.service.os.OperatingSystem;
import org.apache.cayenne.modeler.toolkit.AppFrame;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.toolkit.component.CMComponentGeometryPrefs;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.splitpane.CMSplitPanePrefs;
import org.apache.cayenne.modeler.ui.action.ExitAction;
import org.apache.cayenne.modeler.ui.action.OpenProjectAction;
import org.apache.cayenne.modeler.ui.action.RevertAction;
import org.apache.cayenne.modeler.ui.action.SaveAction;
import org.apache.cayenne.modeler.ui.action.SaveAsAction;
import org.apache.cayenne.modeler.ui.action.ValidateAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.ProjectView;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportResultDialog;
import org.apache.cayenne.modeler.ui.welcome.WelcomeScreen;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Main frame of CayenneModeler GUI. Owns the {@link ProjectSession} for the lifetime of
 * the modeler, mounts the {@link ProjectView} on project-open, and orchestrates project
 * lifecycle (open / save / close).
 */
public class MainFrame extends AppFrame {

    private static final int DEFAULT_BUTTON_WIDTH = 30;
    private static final int DEFAULT_DIVIDER_LOCATION = 400;

    private final GlobalActions globalActions;
    private final ProjectSession session;

    private final JSplitPane splitPane;
    private final CMSplitPanePrefs splitPanePrefs;
    private final JLabel status;
    private final WelcomeScreen welcomePanel;
    private final MainMenuBar menuBar;
    private final DbImportResultDialog dbImportResultDialog;

    private ProjectView projectView;
    private Component dockComponent;

    public MainFrame(Application app) {
        super(app);
        this.globalActions = this.app.getActionManager();

        setIconImage(IconFactory.buildIcon("CayenneModeler.png").getImage());
        getContentPane().setLayout(new BorderLayout());
        this.menuBar = new MainMenuBar(app);
        setJMenuBar(menuBar);
        initToolbar();

        status = new JLabel();
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 10));

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(TopBorder.create());
        splitPane.getInsets().left = 5;
        splitPane.getInsets().right = 5;
        splitPane.setResizeWeight(0.7);

        this.splitPanePrefs = CMSplitPanePrefs.of(app.getPreferencesRepository(), "frame/splitPane");

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        statusBar.setBorder(TopBorder.create());
        // add placeholder
        statusBar.add(Box.createVerticalStrut(16));
        statusBar.add(status);

        if (getContentPane() instanceof JPanel) {
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder());
        }
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        this.welcomePanel = new WelcomeScreen(this.app);
        this.menuBar.addRecentFileListener(welcomePanel);

        fireRecentFileListChanged(); // start filling list in welcome screen and in menu

        setProjectView(null);

        this.app.getPlatformInitializer().setupMenus(this);

        this.session = new ProjectSession(this.app);
        this.session.addDirtyListener((wasDirty, isDirty) -> {
            if (isDirty) {
                onProjectModified();
            }

            globalActions.getAction(SaveAction.class).setEnabled(isDirty);
            globalActions.getAction(SaveAsAction.class).setEnabled(isDirty);
            globalActions.getAction(RevertAction.class).setEnabled(isDirty);
        });

        this.dbImportResultDialog = new DbImportResultDialog(this.app, this);
    }

    public ProjectSession getProjectSession() {
        return session;
    }

    public JLabel getStatus() {
        return status;
    }

    public ProjectView getProjectView() {
        return projectView;
    }

    public DbImportResultDialog getDbImportResultDialog() {
        return dbImportResultDialog;
    }

    private void setProjectView(ProjectView projectView) {
        int oldLocation = splitPane.getDividerLocation();

        this.projectView = projectView;

        if (projectView != null) {
            splitPane.setTopComponent(projectView);
        } else {
            splitPane.setTopComponent(welcomePanel);
        }

        validate();
        splitPane.setDividerLocation(oldLocation);
    }

    /**
     * Plugs a component in the frame, between main area and status bar
     */
    public void setDockComponent(Component c) {
        if (this.dockComponent != c) {

            if (c != null) {
                // re-bind: re-applies the saved divider location so the dock
                // component lands at the user's last position, then resumes tracking
                splitPanePrefs.bind(splitPane, DEFAULT_DIVIDER_LOCATION);
            } else {

                // unbind divider prefs around the swap: while one side of the split pane
                // is missing, Swing's layout shifts the divider to maxLocation, and we
                // don't want that transient value persisted or applied on the way back

                splitPanePrefs.unbind(splitPane);
            }

            this.dockComponent = c;
            splitPane.setBottomComponent(c);
            splitPane.validate();
        }
    }

    /**
     * Notifies all listeners that recent file list has changed.
     */
    public void fireRecentFileListChanged() {
        menuBar.fireRecentFileListChanged();
    }

    public void onStartup() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                session.saveSelectionToPrefs();
                app.getActionManager().getAction(ExitAction.class).exit();
            }
        });

        // Register a hook to save the window position when quit via the app menu. This is in macOS only.
        if (OperatingSystem.getOS() == OperatingSystem.MAC_OS_X) {
            Runnable runner = session::saveSelectionToPrefs;
            Runtime.getRuntime().addShutdownHook(new Thread(runner, "Window Prefs Hook"));
        }

        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(dtde.getDropAction());
                Transferable transferable = dtde.getTransferable();
                dtde.dropComplete(processDropAction(transferable));
            }
        });

        CMComponentGeometryPrefs.of(app.getPreferencesRepository(), "frame/geometry").bind(this, 1200, 720);

        setVisible(true);
    }

    public void onProjectModified() {
        setTitle("* - " + getProjectLocationString());
    }

    public void onProjectSaved() {
        session.setDirty(false);
        updateStatus("Project saved...");
        setTitle(getProjectLocationString());
    }

    public void onProjectClosed() {
        session.saveSelectionToPrefs();

        setProjectView(null);
        this.projectView = null;

        // repaint is needed, since sometimes there is a
        // trace from menu left on the screen
        repaint();
        setTitle("");

        session.projectClosed();
        app.getActionManager().projectClosed();

        updateStatus("Project Closed...");
    }

    /**
     * Handles project opening control. Updates main frame, then delegates control to child controllers.
     */
    public void onProjectOpened(Project project) {

        session.projectOpened(project);
        this.projectView = new ProjectView(session);
        setTitle(getProjectLocationString());
        setProjectView(projectView);

        app.getActionManager().projectOpened();
        session.restoreSelectionFromPrefs();

        // do status update AFTER the project is actually opened...
        if (project.getConfigurationResource() == null) {
            updateStatus("New project created...");
        } else {
            updateStatus("Project opened...");
            fireRecentFileListChanged();
        }

        // for validation purposes combine load failures with post-load validation (not
        // sure if that'll cause duplicate messages?).
        List<ValidationFailure> allFailures = new ArrayList<>();
        Collection<ValidationFailure> loadFailures = project.getConfigurationTree().getLoadFailures();

        if (!loadFailures.isEmpty()) {
            // mark project as unsaved
            project.setModified(true);
            session.setDirty(true);
            allFailures.addAll(loadFailures);
        }

        ProjectValidator projectValidator = app.getProjectValidator();
        ValidationResult validationResult = projectValidator.validate(project.getRootNode());
        allFailures.addAll(validationResult.getFailures());

        if (!allFailures.isEmpty()) {
            app.getActionManager().getAction(ValidateAction.class).showFailures(allFailures);
        }
    }

    /**
     * Adds path to the list of last opened projects in preferences.
     */
    public void addToLastProjListAction(File file) {
        RecentProjectsPrefs.of(app.getPreferencesRepository()).addFile(file);
    }

    public void changePathInLastProjListAction(File oldFile, File newFile) {
        RecentProjectsPrefs prefs = RecentProjectsPrefs.of(app.getPreferencesRepository());
        prefs.removeFile(oldFile);
        prefs.addFile(newFile);

        fireRecentFileListChanged();
    }

    /**
     * Performs status bar update with a message. Message will disappear in 6 seconds.
     */
    public void updateStatus(String message) {
        status.setText(message);

        // start message cleanup thread that would remove the message after X seconds
        if (message != null && !message.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    Thread.sleep(6 * 10000);
                } catch (InterruptedException ignore) {
                }
                if (message.equals(status.getText())) {
                    updateStatus(null);
                }
            }).start();
        }
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
                app.getActionManager().getAction(OpenProjectAction.class).openProject(transferFile);
                return true;
            }
        }

        return false;
    }

    private String getProjectLocationString() {
        if (session.project().getConfigurationResource() == null) {
            return "[New Project]";
        }
        try {
            File projectFile = new File(session.project().getConfigurationResource().getURL().toURI());
            return projectFile.toString();
        } catch (URISyntaxException e) {
            throw new CayenneRuntimeException("Invalid project source URL", e);
        }
    }

    private void initToolbar() {

        MainToolBar toolBar = new MainToolBar(app);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        // Hide some buttons when frame is too small
        addComponentListener(new ComponentAdapter() {
            private final int[] empty = {};
            private final int[] all = {6, 7, 8, 9, 10, 11, 12, 13, 14};
            private final int[] remove = {6, 7};
            private final int[] removeAndCopy = {6, 7, 8, 9, 10, 11};
            private final int[] undo = {12, 13, 14};
            private final int[] undoAndCopy = {8, 9, 10, 11, 12, 13, 14};

            @Override
            public void componentResized(ComponentEvent e) {
                int[] hidden, shown;
                if (getSize().width < (13 * DEFAULT_BUTTON_WIDTH + 300)) {
                    hidden = all;
                    shown = empty;
                } else if (getSize().width < (16 * DEFAULT_BUTTON_WIDTH + 300)) {
                    hidden = removeAndCopy;
                    shown = undo;
                } else if (getSize().width < (18 * DEFAULT_BUTTON_WIDTH + 300)) {
                    hidden = remove;
                    shown = undoAndCopy;
                } else {
                    hidden = empty;
                    shown = all;
                }

                for (int i : hidden) {
                    toolBar.getComponentAtIndex(i).setVisible(false);
                }
                for (int i : shown) {
                    toolBar.getComponentAtIndex(i).setVisible(true);
                }
            }
        });
    }
}

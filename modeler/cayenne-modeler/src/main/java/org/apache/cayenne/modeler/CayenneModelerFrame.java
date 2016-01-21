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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.CopyAction;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.CutAction;
import org.apache.cayenne.modeler.action.DocumentationAction;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.FindAction;
import org.apache.cayenne.modeler.action.GenerateCodeAction;
import org.apache.cayenne.modeler.action.GenerateDBAction;
import org.apache.cayenne.modeler.action.ReverseEngineeringAction;
import org.apache.cayenne.modeler.action.ImportDataMapAction;
import org.apache.cayenne.modeler.action.ImportEOModelAction;
import org.apache.cayenne.modeler.action.InferRelationshipsAction;
import org.apache.cayenne.modeler.action.MigrateAction;
import org.apache.cayenne.modeler.action.NavigateBackwardAction;
import org.apache.cayenne.modeler.action.NavigateForwardAction;
import org.apache.cayenne.modeler.action.NewProjectAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.ProjectAction;
import org.apache.cayenne.modeler.action.RedoAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.action.RevertAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.action.SaveAsAction;
import org.apache.cayenne.modeler.action.ShowLogConsoleAction;
import org.apache.cayenne.modeler.action.UndoAction;
import org.apache.cayenne.modeler.action.ValidateAction;
import org.apache.cayenne.modeler.dialog.LogConsole;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayListener;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayListener;
import org.apache.cayenne.modeler.event.RecentFileListListener;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.RecentFileMenu;
import org.apache.commons.logging.LogFactory;

/**
 * Main frame of CayenneModeler. Responsibilities include coordination of
 * enabling/disabling of menu and toolbar.
 */
public class CayenneModelerFrame extends JFrame implements DataNodeDisplayListener,
        DataMapDisplayListener, ObjEntityDisplayListener, DbEntityDisplayListener,
        QueryDisplayListener, ProcedureDisplayListener, MultipleObjectsDisplayListener,
        EmbeddableDisplayListener {

    protected EditorView view;
    protected RecentFileMenu recentFileMenu;
    protected ActionManager actionManager;
    protected JLabel status;

    /**
     * Menu which shows/hides log console
     */
    protected JCheckBoxMenuItem logMenu;

    /**
     * Split panel, where main project editor and external component, like log console,
     * are located
     */
    protected JSplitPane splitPane;

    /**
     * Component, plugged into this frame
     */
    protected Component dockComponent;

    /**
     * Listeners for changes in recent file menu
     */
    protected List<RecentFileListListener> recentFileListeners;

    /**
     * Welcome screen, shown when no project is open
     */
    protected WelcomeScreen welcomeScreen;

    public CayenneModelerFrame(ActionManager actionManager) {
        super(ModelerConstants.TITLE);
        this.actionManager = actionManager;

        recentFileListeners = new Vector<RecentFileListListener>();

        setIconImage(ModelerUtil.buildIcon("CayenneModeler.jpg").getImage());
        initMenus();
        initToolbar();
        initStatusBar();
        initWelcome();

        fireRecentFileListChanged(); // start filling list in welcome screen and in menu

        setView(null);
    }

    /**
     * Returns an action object associated with the key.
     */
    private <T extends Action> T getAction(Class<T> type) {
        return actionManager.getAction(type);
    }

    protected void initMenus() {
        getContentPane().setLayout(new BorderLayout());

        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu projectMenu = new JMenu("Project");
        JMenu toolMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");

        fileMenu.setMnemonic(KeyEvent.VK_F);
        editMenu.setMnemonic(KeyEvent.VK_E);
        projectMenu.setMnemonic(KeyEvent.VK_P);
        toolMenu.setMnemonic(KeyEvent.VK_T);
        helpMenu.setMnemonic(KeyEvent.VK_H);

        fileMenu.add(getAction(NewProjectAction.class).buildMenu());
        fileMenu.add(getAction(OpenProjectAction.class).buildMenu());
        fileMenu.add(getAction(ProjectAction.class).buildMenu());
        fileMenu.add(getAction(ImportDataMapAction.class).buildMenu());
        fileMenu.addSeparator();
        fileMenu.add(getAction(SaveAction.class).buildMenu());
        fileMenu.add(getAction(SaveAsAction.class).buildMenu());
        fileMenu.add(getAction(RevertAction.class).buildMenu());
        fileMenu.addSeparator();

        editMenu.add(getAction(UndoAction.class).buildMenu());
        editMenu.add(getAction(RedoAction.class).buildMenu());
        editMenu.add(getAction(CutAction.class).buildMenu());
        editMenu.add(getAction(CopyAction.class).buildMenu());
        editMenu.add(getAction(PasteAction.class).buildMenu());

        recentFileMenu = new RecentFileMenu("Recent Projects");
        addRecentFileListListener(recentFileMenu);
        fileMenu.add(recentFileMenu);

        fileMenu.addSeparator();
        fileMenu.add(getAction(ExitAction.class).buildMenu());

        projectMenu.add(getAction(ValidateAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(CreateNodeAction.class).buildMenu());
        projectMenu.add(getAction(CreateDataMapAction.class).buildMenu());

        projectMenu.add(getAction(CreateObjEntityAction.class).buildMenu());
        projectMenu.add(getAction(CreateEmbeddableAction.class).buildMenu());
        projectMenu.add(getAction(CreateDbEntityAction.class).buildMenu());

        projectMenu.add(getAction(CreateProcedureAction.class).buildMenu());
        projectMenu.add(getAction(CreateQueryAction.class).buildMenu());

        projectMenu.addSeparator();
        projectMenu.add(getAction(ObjEntitySyncAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(RemoveAction.class).buildMenu());

        toolMenu.add(getAction(ReverseEngineeringAction.class).buildMenu());
        toolMenu.add(getAction(InferRelationshipsAction.class).buildMenu());
        toolMenu.add(getAction(ImportEOModelAction.class).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(GenerateCodeAction.class).buildMenu());
        toolMenu.add(getAction(GenerateDBAction.class).buildMenu());
        toolMenu.add(getAction(MigrateAction.class).buildMenu());

        /**
         * Menu for opening Log console
         */
        toolMenu.addSeparator();

        logMenu = getAction(ShowLogConsoleAction.class).buildCheckBoxMenu();

        if (!LogConsole.getInstance().getConsoleProperty(LogConsole.DOCKED_PROPERTY)
                && LogConsole.getInstance().getConsoleProperty(
                        LogConsole.SHOW_CONSOLE_PROPERTY)) {
            LogConsole.getInstance().setConsoleProperty(
                    LogConsole.SHOW_CONSOLE_PROPERTY,
                    false);
        }

        updateLogConsoleMenu();
        toolMenu.add(logMenu);

        toolMenu.addSeparator();
        toolMenu.add(getAction(ConfigurePreferencesAction.class).buildMenu());

        helpMenu.add(getAction(AboutAction.class).buildMenu());
        helpMenu.add(getAction(DocumentationAction.class).buildMenu());

        JMenuBar menuBar = new JMenuBar();

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(projectMenu);
        menuBar.add(toolMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Selects/deselects menu item, depending on status of log console
     */
    public void updateLogConsoleMenu() {
        logMenu.setSelected(LogConsole.getInstance().getConsoleProperty(
                LogConsole.SHOW_CONSOLE_PROPERTY));
    }

    protected void initStatusBar() {
        status = new JLabel();
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 10));

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.getInsets().left = 5;
        splitPane.getInsets().right = 5;

        splitPane.setResizeWeight(0.7);

        /**
         * Moving this to try-catch block per CAY-940. Exception will be stack-traced
         */
        try {
            ComponentGeometry geometry = new ComponentGeometry(
                    this.getClass(),
                    "splitPane/divider");
            geometry
                    .bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, 400);
        }
        catch (Exception ex) {
            LogFactory.getLog(getClass()).error("Cannot bind divider property", ex);
        }

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        // add placeholder
        statusBar.add(Box.createVerticalStrut(16));
        statusBar.add(status);

        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Initializes welcome screen
     */
    protected void initWelcome() {
        welcomeScreen = new WelcomeScreen();
        addRecentFileListListener(welcomeScreen);
    }

    /**
     * Plugs a component in the frame, between main area and status bar
     */
    public void setDockComponent(Component c) {
        if (dockComponent == c) {
            return;
        }

        if (dockComponent != null) {
            splitPane.setBottomComponent(null);
        }

        dockComponent = c;

        if (dockComponent != null) {
            splitPane.setBottomComponent(dockComponent);
        }

        splitPane.validate();
    }

    /**
     * @return Dock component
     */
    public Component getDockComponent() {
        return dockComponent;
    }

    /** Initializes main toolbar. */
    protected void initToolbar() {
        JToolBar toolBar = new JToolBar();

        toolBar.add(getAction(NewProjectAction.class).buildButton());
        toolBar.add(getAction(OpenProjectAction.class).buildButton());
        toolBar.add(getAction(SaveAction.class).buildButton());

        toolBar.addSeparator();
        toolBar.add(getAction(RemoveAction.class).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CutAction.class).buildButton());
        toolBar.add(getAction(CopyAction.class).buildButton());
        toolBar.add(getAction(PasteAction.class).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(UndoAction.class).buildButton());
        toolBar.add(getAction(RedoAction.class).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateNodeAction.class).buildButton());
        toolBar.add(getAction(CreateDataMapAction.class).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateDbEntityAction.class).buildButton());
        toolBar.add(getAction(CreateProcedureAction.class).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateObjEntityAction.class).buildButton());
        toolBar.add(getAction(CreateEmbeddableAction.class).buildButton());
        toolBar.add(getAction(CreateQueryAction.class).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(NavigateBackwardAction.class).buildButton());
        toolBar.add(getAction(NavigateForwardAction.class).buildButton());

        JPanel east = new JPanel(new BorderLayout()); // is used to place search feature
        // components the most right on a
        // toolbar
        final JTextField findField = new JTextField(10);
        findField.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                    findField.setBackground(Color.white);
                }
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }

        });
        findField.setAction(getAction(FindAction.class));
        JLabel findLabel = new JLabel("Search:");
        findLabel.setLabelFor(findField);
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            public void eventDispatched(AWTEvent event) {

                if (event instanceof KeyEvent) {

                    if (((KeyEvent) event).getModifiers() == Toolkit
                            .getDefaultToolkit()
                            .getMenuShortcutKeyMask()
                            && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_F) {
                        findField.requestFocus();
                    }
                }
            }

        },
                AWTEvent.KEY_EVENT_MASK);

        JPanel box = new JPanel(); // is used to place label and text field one after
        // another
        box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
        box.add(findLabel);
        box.add(findField);
        east.add(box, BorderLayout.EAST);
        toolBar.add(east);

        getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        actionManager.dataNodeSelected();
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        actionManager.dataMapSelected();
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        actionManager.objEntitySelected();
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        actionManager.dbEntitySelected();
    }

    public void currentQueryChanged(QueryDisplayEvent e) {
        actionManager.querySelected();
    }

    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        actionManager.procedureSelected();
    }

    public void currentObjectsChanged(
            MultipleObjectsDisplayEvent e,
            Application application) {
        actionManager.multipleObjectsSelected(e.getNodes(), application);
    }

    public void currentEmbeddableChanged(EmbeddableDisplayEvent e) {
        actionManager.embeddableSelected();
    }

    /**
     * Returns the right side view panel.
     * 
     * @return EditorView
     */
    public EditorView getView() {
        return view;
    }

    public JLabel getStatus() {
        return status;
    }

    /**
     * Returns the recentFileMenu.
     * 
     * @return RecentFileMenu
     */
    public RecentFileMenu getRecentFileMenu() {
        return recentFileMenu;
    }

    /**
     * Adds editor view to the frame.
     */
    public void setView(EditorView view) {
        int oldLocation = splitPane.getDividerLocation();

        this.view = view;

        if (view != null) {
            splitPane.setTopComponent(view);
        }
        else {
            splitPane.setTopComponent(welcomeScreen);
        }

        validate();
        splitPane.setDividerLocation(oldLocation);
    }

    /**
     * Adds listener for recent menu changes
     */
    public void addRecentFileListListener(RecentFileListListener listener) {
        recentFileListeners.add(listener);
    }

    /**
     * Notifies all listeners that recent file list has changed
     */
    public void fireRecentFileListChanged() {
        for (RecentFileListListener recentFileListener : recentFileListeners) {
            recentFileListener.recentFileListChanged();
        }
    }
}

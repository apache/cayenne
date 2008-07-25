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

import org.apache.cayenne.modeler.action.*;
import org.apache.cayenne.modeler.dialog.LogConsole;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.*;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.OperatingSystem;
import org.apache.cayenne.modeler.util.RecentFileMenu;
import org.apache.cayenne.pref.Domain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;

/**
 * Main frame of CayenneModeler. Responsibilities include coordination of
 * enabling/disabling of menu and toolbar.
 */
public class CayenneModelerFrame extends JFrame implements DataNodeDisplayListener,
        DataMapDisplayListener, ObjEntityDisplayListener, DbEntityDisplayListener,
        QueryDisplayListener, ProcedureDisplayListener, MultipleObjectsDisplayListener {

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
    
    public CayenneModelerFrame(ActionManager actionManager) {
        super(ModelerConstants.TITLE);
        this.actionManager = actionManager;

        initMenus();
        initToolbar();
        initStatusBar();
        
        setView(null);
    }

    /**
     * Returns an action object associated with the key.
     */
    private CayenneAction getAction(String key) {
        return actionManager.getAction(key);
    }

    protected void initMenus() {
        getContentPane().setLayout(new BorderLayout());

        JMenu fileMenu = new JMenu("File");
        JMenu projectMenu = new JMenu("Project");
        JMenu toolMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");

        if (OperatingSystem.getOS() != OperatingSystem.MAC_OS_X) {
            fileMenu.setMnemonic(KeyEvent.VK_F);
            projectMenu.setMnemonic(KeyEvent.VK_P);
            toolMenu.setMnemonic(KeyEvent.VK_T);
            helpMenu.setMnemonic(KeyEvent.VK_H);
        }

        fileMenu.add(getAction(NewProjectAction.getActionName()).buildMenu());
        fileMenu.add(getAction(OpenProjectAction.getActionName()).buildMenu());
        fileMenu.add(getAction(ProjectAction.getActionName()).buildMenu());
        fileMenu.add(getAction(ImportDataMapAction.getActionName()).buildMenu());
        fileMenu.addSeparator();
        fileMenu.add(getAction(SaveAction.getActionName()).buildMenu());
        fileMenu.add(getAction(SaveAsAction.getActionName()).buildMenu());
        fileMenu.add(getAction(RevertAction.getActionName()).buildMenu());
        fileMenu.addSeparator();

        recentFileMenu = new RecentFileMenu("Recent Projects");
        recentFileMenu.rebuildFromPreferences();
        recentFileMenu.setEnabled(recentFileMenu.getMenuComponentCount() > 0);
        fileMenu.add(recentFileMenu);

        // Mac OS X doesn't use File->Exit, it uses CayenneModeler->Quit (command-Q)
        if (OperatingSystem.getOS() != OperatingSystem.MAC_OS_X) {
            fileMenu.addSeparator();
            fileMenu.add(getAction(ExitAction.getActionName()).buildMenu());
        }

        projectMenu.add(getAction(ValidateAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(CreateDomainAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateNodeAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateDataMapAction.getActionName()).buildMenu());

        projectMenu.add(getAction(CreateObjEntityAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateDbEntityAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateProcedureAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateQueryAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(ObjEntitySyncAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(RemoveAction.getActionName()).buildMenu());

        toolMenu.add(getAction(ImportDBAction.getActionName()).buildMenu());
        toolMenu.add(getAction(ImportEOModelAction.getActionName()).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(GenerateCodeAction.getActionName()).buildMenu());
        toolMenu.add(getAction(GenerateDBAction.getActionName()).buildMenu());
        toolMenu.add(getAction(MigrateAction.getActionName()).buildMenu());
        
        /**
         * Menu for opening Log console
         */
        toolMenu.addSeparator();
        
        logMenu = getAction(ShowLogConsoleAction.getActionName()).buildCheckBoxMenu();
        updateLogConsoleMenu();
        toolMenu.add(logMenu);
        
        // Mac OS X has it's own Preferences menu item under the application menu
        if (OperatingSystem.getOS() != OperatingSystem.MAC_OS_X) {
            toolMenu.addSeparator();
            toolMenu.add(getAction(ConfigurePreferencesAction.getActionName()).buildMenu());
        }

        // Mac OS X "About CayenneModeler" appears under the application menu, per Apple GUI standards
        if (OperatingSystem.getOS() != OperatingSystem.MAC_OS_X)
            helpMenu.add(getAction(AboutAction.getActionName()).buildMenu());
        helpMenu.add(getAction(DocumentationAction.getActionName()).buildMenu());
        
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(fileMenu);
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
        
        Domain domain = Application.getInstance().getPreferenceDomain().getSubdomain(
                this.getClass());
        ComponentGeometry geometry = (ComponentGeometry) domain.getDetail(
                "splitPane.divider",
                ComponentGeometry.class,
                true);
        geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, 400);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        // add placeholder
        statusBar.add(Box.createVerticalStrut(16));
        statusBar.add(status);

        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
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

        toolBar.add(getAction(NewProjectAction.getActionName()).buildButton());
        toolBar.add(getAction(OpenProjectAction.getActionName()).buildButton());
        toolBar.add(getAction(SaveAction.getActionName()).buildButton());
        toolBar.add(getAction(RemoveAction.getActionName()).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateDomainAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateNodeAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateDataMapAction.getActionName()).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateDbEntityAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateProcedureAction.getActionName()).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(CreateObjEntityAction.getActionName()).buildButton());
        toolBar.add(getAction(CreateQueryAction.getActionName()).buildButton());

        toolBar.addSeparator();

        toolBar.add(getAction(NavigateBackwardAction.getActionName()).buildButton());
        toolBar.add(getAction(NavigateForwardAction.getActionName()).buildButton());

        JPanel east = new JPanel(new BorderLayout());   // is used to place search feature components the most right on a toolbar  
        final JTextField findField = new JTextField(10);
        findField.setAction(getAction(FindAction.getActionName()));
        JLabel findLabel = new JLabel("Search:");
        findLabel.setLabelFor(findField);
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                if (event instanceof KeyEvent) {
                    if (((KeyEvent) event).getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                            && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_F)
                                findField.requestFocus();
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
        JPanel box = new JPanel();  // is used to place label and text field one after another
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
    
    public void currentObjectsChanged(MultipleObjectsDisplayEvent e) {
        actionManager.multipleObjectsSelected();
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
            splitPane.setTopComponent(new JPanel());
        }

        validate();
        splitPane.setDividerLocation(oldLocation);
    }
}

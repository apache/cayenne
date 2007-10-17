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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.AWTEventListener;

import javax.swing.*;
import javax.swing.border.Border;

import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateDerivedDbEntityAction;
import org.apache.cayenne.modeler.action.CreateDomainAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.DerivedEntitySyncAction;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.FindAction;
import org.apache.cayenne.modeler.action.GenerateCodeAction;
import org.apache.cayenne.modeler.action.GenerateDBAction;
import org.apache.cayenne.modeler.action.ImportDBAction;
import org.apache.cayenne.modeler.action.ImportDataMapAction;
import org.apache.cayenne.modeler.action.ImportEOModelAction;
import org.apache.cayenne.modeler.action.NavigateBackwardAction;
import org.apache.cayenne.modeler.action.NavigateForwardAction;
import org.apache.cayenne.modeler.action.NewProjectAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.action.ProjectAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.action.RevertAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.action.SaveAsAction;
import org.apache.cayenne.modeler.action.ValidateAction;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayListener;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.RecentFileMenu;
import org.apache.commons.lang.SystemUtils;

/**
 * Main frame of CayenneModeler. Responsibilities include coordination of
 * enabling/disabling of menu and toolbar.
 */
public class CayenneModelerFrame extends JFrame implements DataNodeDisplayListener,
        DataMapDisplayListener, ObjEntityDisplayListener, DbEntityDisplayListener,
        QueryDisplayListener, ProcedureDisplayListener {

    protected EditorView view;
    protected RecentFileMenu recentFileMenu;
    protected ActionManager actionManager;
    protected JLabel status;

    public CayenneModelerFrame(ActionManager actionManager) {
        super(ModelerConstants.TITLE);
        this.actionManager = actionManager;

        initMenus();
        initToolbar();
        initStatusBar();
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

        if (!SystemUtils.IS_OS_MAC_OSX) {
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

        recentFileMenu = new RecentFileMenu("Recent Files");
        recentFileMenu.rebuildFromPreferences();
        recentFileMenu.setEnabled(recentFileMenu.getMenuComponentCount() > 0);
        fileMenu.add(recentFileMenu);

        fileMenu.addSeparator();
        fileMenu.add(getAction(ExitAction.getActionName()).buildMenu());

        projectMenu.add(getAction(ValidateAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(CreateDomainAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateNodeAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateDataMapAction.getActionName()).buildMenu());

        projectMenu.add(getAction(CreateObjEntityAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateDbEntityAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateDerivedDbEntityAction.getActionName())
                .buildMenu());
        projectMenu.add(getAction(CreateProcedureAction.getActionName()).buildMenu());
        projectMenu.add(getAction(CreateQueryAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(ObjEntitySyncAction.getActionName()).buildMenu());
        projectMenu.add(getAction(DerivedEntitySyncAction.getActionName()).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(getAction(RemoveAction.getActionName()).buildMenu());

        toolMenu.add(getAction(ImportDBAction.getActionName()).buildMenu());
        toolMenu.add(getAction(ImportEOModelAction.getActionName()).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(GenerateCodeAction.getActionName()).buildMenu());
        toolMenu.add(getAction(GenerateDBAction.getActionName()).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(getAction(ConfigurePreferencesAction.getActionName()).buildMenu());

        helpMenu.add(getAction(AboutAction.getActionName()).buildMenu());

        JMenuBar menuBar = new JMenuBar();

        menuBar.add(fileMenu);
        menuBar.add(projectMenu);
        menuBar.add(toolMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    protected void initStatusBar() {
        status = new JLabel();
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 10));

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        // add placeholder
        statusBar.add(Box.createVerticalStrut(16));
        statusBar.add(status);

        getContentPane().add(statusBar, BorderLayout.SOUTH);
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
        toolBar.add(getAction(CreateDerivedDbEntityAction.getActionName()).buildButton());
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
        boolean derived = e.getEntity() instanceof DerivedDbEntity;

        if (derived) {
            actionManager.derivedDbEntitySelected();
            getAction(DerivedEntitySyncAction.getActionName()).setEnabled(true);
        }
        else {
            actionManager.dbEntitySelected();
        }
    }

    public void currentQueryChanged(QueryDisplayEvent e) {
        actionManager.querySelected();
    }

    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        actionManager.procedureSelected();
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
        boolean change = false;

        if (this.view != null) {
            getContentPane().remove(this.view);
            change = true;
        }

        this.view = view;

        if (view != null) {
            getContentPane().add(view, BorderLayout.CENTER);
            change = true;
        }

        if (change) {
            validate();
        }
    }
}

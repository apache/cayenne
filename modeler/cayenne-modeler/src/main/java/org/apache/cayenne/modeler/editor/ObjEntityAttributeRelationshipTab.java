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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntityCounterpartAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.components.image.FilteredIconFactory;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;

/**
 * Combines ObjEntityAttributeTab and ObjEntityRelationshipTab in JSplitPane.
 */

public class ObjEntityAttributeRelationshipTab extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener {

    public ObjEntityAttributePanel attributePanel;
    public ObjEntityRelationshipPanel relationshipPanel;
    public JButton resolve = new CayenneAction.CayenneToolbarButton(null, 0);
    private JSplitPane splitPane;

    private ProjectController mediator;

    private CutAttributeRelationshipAction cut;
    private RemoveAttributeRelationshipAction remove;
    private CopyAttributeRelationshipAction copy;
    private JToolBar toolBar;

    public ObjEntityAttributeRelationshipTab(ProjectController mediator) {
        this.mediator = mediator;

        init();
        initToolBar();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        attributePanel = new ObjEntityAttributePanel(mediator, this);
        relationshipPanel = new ObjEntityRelationshipPanel(mediator, this);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, attributePanel, relationshipPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);

        try {
            ComponentGeometry geometry = new ComponentGeometry(
                    this.getClass(),
                    "objEntityAttrRelTab/splitPane/divider");

            geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, -1);
        }
        catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Cannot bind divider property", ex);
        }

        add(splitPane);
    }

    private void initToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        ActionManager actionManager = Application.getInstance().getActionManager();

        toolBar.add(actionManager.getAction(CreateAttributeAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();
        toolBar.add(actionManager.getAction(ObjEntitySyncAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(ObjEntityCounterpartAction.class).buildButton(3));
        toolBar.addSeparator();

        Icon ico = ModelerUtil.buildIcon("icon-edit.png");
        resolve.setToolTipText("Edit");
        resolve.setIcon(ico);
        resolve.setDisabledIcon(FilteredIconFactory.createDisabledIcon(ico));
        toolBar.add(resolve).setEnabled(false);

        cut = actionManager.getAction(CutAttributeRelationshipAction.class);
        remove = actionManager.getAction(RemoveAttributeRelationshipAction.class);
        copy = actionManager.getAction(CopyAttributeRelationshipAction.class);

        toolBar.addSeparator();
        toolBar.add(remove.buildButton());
        toolBar.addSeparator();
        toolBar.add(cut.buildButton(1));
        toolBar.add(copy.buildButton(2));
        toolBar.add(actionManager.getAction(PasteAction.class).buildButton(3));

        add(toolBar, BorderLayout.NORTH);
    }

    public void updateActions(Object[] params) {
        ModelerUtil.updateActions(
                params.length,
                RemoveAttributeRelationshipAction.class,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
        resolve.setEnabled(params.length > 0);
    }

    public JButton getResolve() {
        return resolve;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public ObjEntityAttributePanel getAttributePanel() {
        return attributePanel;
    }

    public ObjEntityRelationshipPanel getRelationshipPanel() {
        return relationshipPanel;
    }

    public void objEntityChanged(EntityEvent e) {
        attributePanel.objEntityChanged(e);
        relationshipPanel.objEntityChanged(e);
    }

    public void objEntityAdded(EntityEvent e) {
        attributePanel.objEntityAdded(e);
        relationshipPanel.objEntityAdded(e);
    }

    public void objEntityRemoved(EntityEvent e) {
        attributePanel.objEntityRemoved(e);
        relationshipPanel.objEntityRemoved(e);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        attributePanel.currentObjEntityChanged(e);
        relationshipPanel.currentObjEntityChanged(e);
    }

    public JToolBar getToolBar() {
        return toolBar;
    }
}

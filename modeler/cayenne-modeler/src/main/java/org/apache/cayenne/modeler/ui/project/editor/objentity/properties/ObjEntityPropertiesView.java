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
package org.apache.cayenne.modeler.ui.project.editor.objentity.properties;

import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntityCounterpartAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.action.ModelerAbstractAction;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.swing.image.FilteredIconFactory;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;

/**
 * Combines ObjEntityAttributeTab and ObjEntityRelationshipTab in JSplitPane.
 */
public class ObjEntityPropertiesView extends JPanel implements ObjEntityDisplayListener, ObjEntityListener {

    private final ObjAttributePanel attributePanel;
    private final ObjRelationshipPanel relationshipPanel;
    private final JSplitPane splitPane;
    private final JToolBar toolBar;
    private final JButton editButton;

    public ObjEntityPropertiesView(ProjectController controller) {
        this.setLayout(new BorderLayout());

        this.editButton = new ModelerAbstractAction.CayenneToolbarButton(null, 0);
        attributePanel = new ObjAttributePanel(controller, this);
        relationshipPanel = new ObjRelationshipPanel(controller, this);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, attributePanel, relationshipPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);

        try {
            ComponentGeometry geometry = new ComponentGeometry(
                    this.getClass(),
                    "objEntityAttrRelTab/splitPane/divider");

            geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, -1);
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Cannot bind divider property", ex);
        }

        add(splitPane);

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
        editButton.setToolTipText("Edit");
        editButton.setIcon(ico);
        editButton.setDisabledIcon(FilteredIconFactory.createDisabledIcon(ico));
        toolBar.add(editButton).setEnabled(false);

        toolBar.addSeparator();
        toolBar.add(actionManager.getAction(RemoveAttributeRelationshipAction.class).buildButton());
        toolBar.addSeparator();
        toolBar.add(actionManager.getAction(CutAttributeRelationshipAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(CopyAttributeRelationshipAction.class).buildButton(2));
        toolBar.add(actionManager.getAction(PasteAction.class).buildButton(3));

        add(toolBar, BorderLayout.NORTH);
    }

    public void updateActions(Object[] params) {
        ModelerUtil.updateActions(
                params.length,
                RemoveAttributeRelationshipAction.class,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
        editButton.setEnabled(params.length > 0);
    }

    public void rebindEditButton(boolean enabled, String tooltipText, ActionListener action) {
        for (ActionListener al : editButton.getActionListeners()) {
            editButton.removeActionListener(al);
        }
        editButton.addActionListener(action);
        editButton.setToolTipText(tooltipText);
        editButton.setEnabled(enabled);
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public ObjAttributePanel getAttributePanel() {
        return attributePanel;
    }

    public ObjRelationshipPanel getRelationshipPanel() {
        return relationshipPanel;
    }

    @Override
    public void objEntityChanged(EntityEvent e) {
        attributePanel.objEntityChanged(e);
        relationshipPanel.objEntityChanged(e);
    }

    @Override
    public void objEntityAdded(EntityEvent e) {
        attributePanel.objEntityAdded(e);
        relationshipPanel.objEntityAdded(e);
    }

    @Override
    public void objEntityRemoved(EntityEvent e) {
        attributePanel.objEntityRemoved(e);
        relationshipPanel.objEntityRemoved(e);
    }

    @Override
    public void objEntitySelected(EntityDisplayEvent e) {
        attributePanel.objEntitySelected(e);
        relationshipPanel.objEntitySelected(e);
    }

    public JToolBar getToolBar() {
        return toolBar;
    }
}

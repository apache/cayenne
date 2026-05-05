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

package org.apache.cayenne.modeler.ui.project.editor.objentity;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.event.display.ObjAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjRelationshipDisplayEvent;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.ui.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.ObjEntityCallbacksView;
import org.apache.cayenne.modeler.ui.project.editor.objentity.main.ObjEntityMainView;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjAttributePanel;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjEntityPropertiesView;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjRelationshipPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class ObjEntityView extends ProjectTabbedPane {

    private final Component entityPanel;
    private final ObjEntityPropertiesView attributeRelationshipTab;
    private final ObjEntityCallbacksView callbacksPanel;
    private int lastTabIndex;

    public ObjEntityView(ProjectSession session) {
        super(session);
        // note that those panels that have no internal scrollable tables must be wrapped in a scroll pane
        entityPanel = new JScrollPane(new ObjEntityMainView(session));
        attributeRelationshipTab = new ObjEntityPropertiesView(session);
        callbacksPanel = new ObjEntityCallbacksView(session);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setTabPlacement(JTabbedPane.TOP);
        addTab("Entity", entityPanel);
        addTab("Properties", attributeRelationshipTab);
        addTab("Callbacks", callbacksPanel);
    }

    private void initBindings() {
        session.addObjEntityDisplayListener(this::currentObjEntityChanged);
        session.addObjAttributeDisplayListener(this::currentObjAttributeChanged);
        session.addObjRelationshipDisplayListener(this::currentObjRelationshipChanged);
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        resetRemoveButtons();

        lastTabIndex = getSelectedIndex();

        Component selected = getSelectedComponent();
        while (selected instanceof JScrollPane) {
            selected = ((JScrollPane) selected).getViewport().getView();
        }

        if (selected instanceof ExistingSelectionProcessor) {
            ((ExistingSelectionProcessor) selected).processExistingSelection(e);
        }
    }

    private void resetRemoveButtons() {
        GlobalActions globalActions = app.getActionManager();

        globalActions.getAction(RemoveAttributeAction.class).setEnabled(false);
        globalActions.getAction(RemoveRelationshipAction.class).setEnabled(false);
        globalActions.getAction(RemoveCallbackMethodAction.class).setEnabled(false);
    }

    private void currentObjEntityChanged(ObjEntityDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        if (e.isMainTabFocus()) {
            if (getSelectedComponent() != entityPanel) {
                setSelectedComponent(entityPanel);
                entityPanel.setVisible(true);
            }
        }

        resetRemoveButtons();
        setVisible(true);

        if (getRootPane() != null) {
            setSelectedIndex(lastTabIndex);
        }
    }

    private void currentObjRelationshipChanged(ObjRelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        ObjRelationshipPanel relationshipPanel = (ObjRelationshipPanel) attributeRelationshipTab.getSplitPane().getComponent(1);
        ObjRelationship[] objRels = e.getRelationships();

        if (getSelectedComponent() != relationshipPanel && objRels.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            relationshipPanel.setVisible(true);
        }

        relationshipPanel.selectRelationships(objRels);
        attributeRelationshipTab.updateActions(objRels);
    }

    private void currentObjAttributeChanged(ObjAttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update attribute selection
        ObjAttributePanel attributePanel = (ObjAttributePanel) attributeRelationshipTab.getSplitPane().getComponent(0);
        ObjAttribute[] objAttrs = e.getAttributes();

        if (getSelectedComponent() != attributePanel && objAttrs.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributePanel.setVisible(true);
        }

        attributePanel.selectAttributes(objAttrs);
        attributeRelationshipTab.updateActions(objAttrs);
    }
}

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

import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.ui.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.event.display.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.ObjEntityCallbacksView;
import org.apache.cayenne.modeler.ui.project.editor.objentity.main.ObjEntityMainView;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjAttributePanel;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjEntityPropertiesView;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjRelationshipPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class ObjEntityView extends JTabbedPane {

    private final ProjectController controller;
    private final Component entityPanel;
    private final ObjEntityPropertiesView attributeRelationshipTab;
    private final ObjEntityCallbacksView callbacksPanel;
    private int lastTabIndex;

    public ObjEntityView(ProjectController controller) {
        this.controller = controller;

        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane

        entityPanel = new JScrollPane(new ObjEntityMainView(controller));
        addTab("Entity", entityPanel);

        attributeRelationshipTab = new ObjEntityPropertiesView(controller);
        addTab("Properties", attributeRelationshipTab);

        callbacksPanel = new ObjEntityCallbacksView(controller);
        addTab("Callbacks", callbacksPanel);

        controller.addObjEntityDisplayListener(this::currentObjEntityChanged);
        controller.addObjAttributeDisplayListener(this::currentObjAttributeChanged);
        controller.addObjRelationshipDisplayListener(this::currentObjRelationshipChanged);

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
        GlobalActions globalActions = controller.getApplication().getActionManager();

        globalActions.getAction(RemoveAttributeAction.class).setEnabled(false);
        globalActions.getAction(RemoveRelationshipAction.class).setEnabled(false);
        globalActions.getAction(RemoveCallbackMethodAction.class).setEnabled(false);
    }

    private void currentObjEntityChanged(EntityDisplayEvent e) {
        Entity<?, ?, ?> entity = e.getEntity();
        if (!(entity instanceof ObjEntity)) {
            return;
        }

        if (e.isMainTabFocus()) {
            if (getSelectedComponent() != entityPanel) {
                setSelectedComponent(entityPanel);
                entityPanel.setVisible(true);
            }
        }

        resetRemoveButtons();
        setVisible(e.getEntity() != null);

        if (getRootPane() != null) {
            setSelectedIndex(lastTabIndex);
        }
    }

    private void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        Relationship<?, ?, ?>[] rels = e.getRelationships();
        ObjRelationship[] objRels = new ObjRelationship[rels.length];

        System.arraycopy(rels, 0, objRels, 0, rels.length);

        if (getSelectedComponent() != attributeRelationshipTab.getSplitPane().getComponent(1) && objRels.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributeRelationshipTab.getSplitPane().getComponent(1).setVisible(true);
        }

        ((ObjRelationshipPanel) attributeRelationshipTab.getSplitPane().getComponent(1)).selectRelationships(objRels);
        attributeRelationshipTab.updateActions(objRels);
    }

    private void currentObjAttributeChanged(AttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update attribute selection
        Attribute<?, ?, ?>[] attrs = e.getAttributes();
        ObjAttribute[] objAttrs = new ObjAttribute[attrs.length];

        System.arraycopy(attrs, 0, objAttrs, 0, attrs.length);

        if (getSelectedComponent() != attributeRelationshipTab.getSplitPane().getComponent(0) && objAttrs.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributeRelationshipTab.getSplitPane().getComponent(0).setVisible(true);
        }

        ((ObjAttributePanel) attributeRelationshipTab.getSplitPane().getComponent(0)).selectAttributes(objAttrs);
        attributeRelationshipTab.updateActions(objAttrs);
    }
}

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

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjAttributeDisplayListener;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ObjRelationshipDisplayListener;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;

/**
 * Tabbed ObjEntity editor panel.
 * 
 */
public class ObjEntityTabbedView extends JTabbedPane implements ObjEntityDisplayListener,
        ObjRelationshipDisplayListener, ObjAttributeDisplayListener {

    protected ProjectController projectController;

    protected Component entityPanel;
    protected ObjEntityAttributeRelationshipTab attributeRelationshipTab;

    /**
     * callback methods on ObjEntity tab
     */
    protected AbstractCallbackMethodsTab callbacksPanel;

    public ObjEntityTabbedView(ProjectController projectController) {
        super();
        this.projectController = projectController;

        initView();
        initController();
    }

    private void initView() {
        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane

        entityPanel = new JScrollPane(new ObjEntityTab(projectController));
        addTab("Entity", entityPanel);

        attributeRelationshipTab = new ObjEntityAttributeRelationshipTab(projectController);
        addTab("Properties", attributeRelationshipTab);

        callbacksPanel = new ObjEntityCallbackMethodsTab(projectController);
        addTab("Callbacks", callbacksPanel);
    }

    private void initController() {
        projectController.addObjEntityDisplayListener(this);
        projectController.addObjAttributeDisplayListener(this);
        projectController.addObjRelationshipDisplayListener(this);

        addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                resetRemoveButtons();

                projectController.setEntityTabSelection(getSelectedIndex());

                Component selected = getSelectedComponent();
                while (selected instanceof JScrollPane) {
                    selected = ((JScrollPane) selected).getViewport().getView();
                }

                if (selected instanceof ExistingSelectionProcessor) {
                    ((ExistingSelectionProcessor) selected).processExistingSelection(e);
                }
            }
        });
    }

    /** Reset the remove buttons */
    private void resetRemoveButtons() {
        ActionManager actionManager = Application.getInstance().getActionManager();

        actionManager.getAction(RemoveAttributeAction.class).setEnabled(false);
        actionManager.getAction(RemoveRelationshipAction.class).setEnabled(false);
        actionManager.getAction(RemoveCallbackMethodAction.class).setEnabled(false);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        Entity entity = e.getEntity();

        if (e.isMainTabFocus() && entity instanceof ObjEntity) {
            if (getSelectedComponent() != entityPanel) {
                setSelectedComponent(entityPanel);
                entityPanel.setVisible(true);
            }
        }

        resetRemoveButtons();
        setVisible(e.getEntity() != null);

        if (getRootPane() != null) {
            if (projectController.getEntityTabSelection() < getTabCount()) {
                setSelectedIndex(projectController.getEntityTabSelection());
            }
        }
    }

    public void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        Relationship[] rels = e.getRelationships();
        ObjRelationship[] objRels = new ObjRelationship[rels.length];

        System.arraycopy(rels, 0, objRels, 0, rels.length);

        if (getSelectedComponent() != attributeRelationshipTab.getSplitPane().getComponent(1) && objRels.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributeRelationshipTab.getSplitPane().getComponent(1).setVisible(true);
        }

        ((ObjEntityRelationshipPanel) attributeRelationshipTab.getSplitPane().getComponent(1)).selectRelationships(objRels);
        attributeRelationshipTab.updateActions(objRels);
    }

    public void currentObjAttributeChanged(AttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update attribute selection
        Attribute[] attrs = e.getAttributes();
        ObjAttribute[] objAttrs = new ObjAttribute[attrs.length];

        System.arraycopy(attrs, 0, objAttrs, 0, attrs.length);

        if (getSelectedComponent() != attributeRelationshipTab.getSplitPane().getComponent(0) && objAttrs.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributeRelationshipTab.getSplitPane().getComponent(0).setVisible(true);
        }

        ((ObjEntityAttributePanel) attributeRelationshipTab.getSplitPane().getComponent(0)).selectAttributes(objAttrs);
        attributeRelationshipTab.updateActions(objAttrs);
    }

    public ObjEntityAttributeRelationshipTab getAttributeRelationshipTab() {
        return attributeRelationshipTab;
    }
}

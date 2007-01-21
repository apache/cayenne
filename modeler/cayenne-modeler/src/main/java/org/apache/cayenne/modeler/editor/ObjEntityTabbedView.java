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

package org.apache.cayenne.modeler.editor;

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjAttributeDisplayListener;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ObjRelationshipDisplayListener;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;

/**
 * Tabbed ObjEntity editor panel.
 * 
 * @author Michael Misha Shengaout
 * @author Andrus Adamchik
 */
public class ObjEntityTabbedView extends JTabbedPane implements ObjEntityDisplayListener,
        ObjRelationshipDisplayListener, ObjAttributeDisplayListener {

    protected ProjectController mediator;
    protected ObjEntityRelationshipTab relationshipsPanel;
    protected ObjEntityAttributeTab attributesPanel;

    public ObjEntityTabbedView(ProjectController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {
        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane

        ObjEntityTab entityPanel = new ObjEntityTab(mediator);
        addTab("Entity", new JScrollPane(entityPanel));
        
        attributesPanel = new ObjEntityAttributeTab(mediator);
        addTab("Attributes", attributesPanel);
        
        relationshipsPanel = new ObjEntityRelationshipTab(mediator);
        addTab("Relationships", relationshipsPanel);
    }

    private void initController() {
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjAttributeDisplayListener(this);
        mediator.addObjRelationshipDisplayListener(this);

        addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                resetRemoveButtons();
                Component selected = getSelectedComponent();
                while (selected instanceof JScrollPane) {
                    selected = ((JScrollPane) selected).getViewport().getView();
                }

                ((ExistingSelectionProcessor) selected).processExistingSelection(e);
            }
        });
    }

    /** Reset the remove buttons */
    private void resetRemoveButtons(){
        Application app = Application.getInstance();
        app.getAction(RemoveAttributeAction.getActionName()).setEnabled(false);
        app.getAction(RemoveRelationshipAction.getActionName()).setEnabled(false);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        resetRemoveButtons();
        setVisible(e.getEntity() != null);
    }

    public void currentObjRelationshipChanged(RelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        Relationship rel = e.getRelationship();
        if (rel instanceof ObjRelationship) {
            if (getSelectedComponent() != relationshipsPanel) {
                setSelectedComponent(relationshipsPanel);
                relationshipsPanel.setVisible(true);
            }
            relationshipsPanel.selectRelationship((ObjRelationship) rel);
        }
    }

    public void currentObjAttributeChanged(AttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update relationship selection
        Attribute attr = e.getAttribute();
        if (attr instanceof ObjAttribute) {
            if (getSelectedComponent() != attributesPanel) {
                setSelectedComponent(attributesPanel);
                attributesPanel.setVisible(true);
            }
            attributesPanel.selectAttribute((ObjAttribute) attr);
        }
    }
}

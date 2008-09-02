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

package org.apache.cayenne.modeler.editor.dbentity;

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.editor.ExistingSelectionProcessor;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.DbAttributeDisplayListener;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.DbRelationshipDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;

public class DbEntityTabbedView extends JTabbedPane implements ChangeListener,
        DbEntityDisplayListener, DbRelationshipDisplayListener,
        DbAttributeDisplayListener {

    protected ProjectController mediator;

    protected Component entityPanel;
    protected DbEntityAttributeTab attributesPanel;
    protected DbEntityRelationshipTab relationshipsPanel;

    public DbEntityTabbedView(ProjectController mediator) {
        super();
        this.mediator = mediator;
        mediator.addDbEntityDisplayListener(this);
        mediator.addDbAttributeDisplayListener(this);
        mediator.addDbRelationshipDisplayListener(this);

        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane

        entityPanel = new JScrollPane(new DbEntityTab(mediator));
        addTab("Entity", entityPanel);
        attributesPanel = new DbEntityAttributeTab(mediator);
        addTab("Attributes", attributesPanel);
        relationshipsPanel = new DbEntityRelationshipTab(mediator);
        addTab("Relationships", relationshipsPanel);

        addChangeListener(this);
    }

    /** Reset the remove buttons */
    private void resetRemoveButtons(){
        Application app = Application.getInstance();
        app.getAction(RemoveAttributeAction.getActionName()).setEnabled(false);
        app.getAction(RemoveRelationshipAction.getActionName()).setEnabled(false);
    }
    
    /** Handle focus when tab changes. */
    public void stateChanged(ChangeEvent e) {
        resetRemoveButtons();
        
        // find source view
        Component selected = getSelectedComponent();
        while (selected instanceof JScrollPane) {
            selected = ((JScrollPane) selected).getViewport().getView();
        }

        ExistingSelectionProcessor proc = (ExistingSelectionProcessor) selected;
        proc.processExistingSelection(e);
    }

    /** If entity is null hides it's contents, otherwise makes it visible. */
    public void currentDbEntityChanged(EntityDisplayEvent e) {
        Entity entity = e.getEntity();
        if (e.isMainTabFocus() && entity instanceof DbEntity) {
            if (getSelectedComponent() != entityPanel) {
                setSelectedComponent(entityPanel);
                entityPanel.setVisible(true);
            }
        }
        
        resetRemoveButtons();
        setVisible(e.getEntity() != null);
    }

    public void currentDbRelationshipChanged(RelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }
        
        // update relationship selection
        Relationship[] rels = e.getRelationships();
        DbRelationship[] dbRels = new DbRelationship[rels.length];
        
        System.arraycopy(rels, 0, dbRels, 0, rels.length);
        
        // reset tab to relationship
        if (getSelectedComponent() != relationshipsPanel && dbRels.length > 0) {
            setSelectedComponent(relationshipsPanel);
            relationshipsPanel.setVisible(true);
        }
        
        relationshipsPanel.selectRelationships(dbRels);
    }

    public void currentDbAttributeChanged(AttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update relationship selection
        Attribute[] attrs = e.getAttributes();
        DbAttribute[] dbAttrs = new DbAttribute[attrs.length];
        
        System.arraycopy(attrs, 0, dbAttrs, 0, attrs.length);
        
        if (getSelectedComponent() != attributesPanel && dbAttrs.length > 0) {
            setSelectedComponent(attributesPanel);
            attributesPanel.setVisible(true);
        }
            
        attributesPanel.selectAttributes(dbAttrs);
    }
}

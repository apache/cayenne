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

package org.apache.cayenne.modeler.ui.project.editor.dbentity;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.event.display.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.main.DbEntityMainView;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.properties.DbAttributePanel;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.properties.DbEntityPropertiesView;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.properties.DbRelationshipPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class DbEntityView extends JTabbedPane {

    private final Component entityPanel;
    private final DbEntityPropertiesView attributeRelationshipTab;
    private int lastTabIndex;

    public DbEntityView(ProjectController controller) {

        setTabPlacement(JTabbedPane.TOP);

        this.entityPanel = new JScrollPane(new DbEntityMainView(controller));
        addTab("Entity", entityPanel);

        this.attributeRelationshipTab = new DbEntityPropertiesView(controller);
        addTab("Properties", attributeRelationshipTab);

        controller.addDbEntityDisplayListener(this::currentDbEntityChanged);
        controller.addDbAttributeDisplayListener(this::currentDbAttributeChanged);
        controller.addDbRelationshipDisplayListener(this::currentDbRelationshipChanged);

        addChangeListener(this::stateChanged);
    }

    private void resetRemoveButtons() {
        GlobalActions globalActions = Application.getInstance().getActionManager();

        globalActions.getAction(RemoveAttributeAction.class).setEnabled(false);
        globalActions.getAction(RemoveRelationshipAction.class).setEnabled(false);
    }

    private void stateChanged(ChangeEvent e) {
        resetRemoveButtons();
        lastTabIndex = getSelectedIndex();
    }

    private void currentDbEntityChanged(EntityDisplayEvent e) {
        Entity<?, ?, ?> entity = e.getEntity();
        if (!(entity instanceof DbEntity)) {
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
        setSelectedIndex(lastTabIndex);
    }

    private void currentDbRelationshipChanged(RelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        Relationship[] rels = e.getRelationships();
        DbRelationship[] dbRels = new DbRelationship[rels.length];

        System.arraycopy(rels, 0, dbRels, 0, rels.length);

        // reset tab to relationship
        if (getSelectedComponent() != attributeRelationshipTab.getSplitPane().getComponent(1) && dbRels.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributeRelationshipTab.getSplitPane().getComponent(1).setVisible(true);
        }

        ((DbRelationshipPanel) attributeRelationshipTab.getSplitPane().getComponent(1)).selectRelationships(dbRels);
        attributeRelationshipTab.updateActions(dbRels);
    }

    private void currentDbAttributeChanged(AttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update attribute selection
        Attribute<?, ?, ?>[] attrs = e.getAttributes();
        DbAttribute[] dbAttrs = new DbAttribute[attrs.length];

        System.arraycopy(attrs, 0, dbAttrs, 0, attrs.length);

        if (getSelectedComponent() != attributeRelationshipTab.getSplitPane().getComponent(0) && dbAttrs.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributeRelationshipTab.getSplitPane().getComponent(0).setVisible(true);
        }

        ((DbAttributePanel) attributeRelationshipTab.getSplitPane().getComponent(0)).selectAttributes(dbAttrs);
        attributeRelationshipTab.updateActions(dbAttrs);
    }
}

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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.event.display.DbAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbRelationshipDisplayEvent;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.main.DbEntityMainView;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.properties.DbAttributePanel;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.properties.DbEntityPropertiesView;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.properties.DbRelationshipPanel;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import java.awt.Component;

public class DbEntityView extends ProjectTabbedPane {

    private final Component entityPanel;
    private final DbEntityPropertiesView attributeRelationshipTab;
    private int lastTabIndex;

    public DbEntityView(ProjectSession session) {
        super(session);
        this.entityPanel = new JScrollPane(new DbEntityMainView(session));
        this.attributeRelationshipTab = new DbEntityPropertiesView(session);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setTabPlacement(JTabbedPane.TOP);
        addTab("Entity", entityPanel);
        addTab("Properties", attributeRelationshipTab);
    }

    private void initBindings() {
        session.addDbEntityDisplayListener(this::currentDbEntityChanged);
        session.addDbAttributeDisplayListener(this::currentDbAttributeChanged);
        session.addDbRelationshipDisplayListener(this::currentDbRelationshipChanged);
        addChangeListener(this::stateChanged);
    }

    private void resetRemoveButtons() {
        GlobalActions globalActions = app.getActionManager();

        globalActions.getAction(RemoveAttributeAction.class).setEnabled(false);
        globalActions.getAction(RemoveRelationshipAction.class).setEnabled(false);
    }

    private void stateChanged(ChangeEvent e) {
        resetRemoveButtons();
        lastTabIndex = getSelectedIndex();
    }

    private void currentDbEntityChanged(DbEntityDisplayEvent e) {
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
        setSelectedIndex(lastTabIndex);
    }

    private void currentDbRelationshipChanged(DbRelationshipDisplayEvent e) {
        if (e.getEntity() == null) {
            return;
        }

        // update relationship selection
        DbRelationshipPanel relationshipPanel = (DbRelationshipPanel) attributeRelationshipTab.getSplitPane().getComponent(1);
        DbRelationship[] dbRels = e.getRelationships();

        // reset tab to relationship
        if (getSelectedComponent() != relationshipPanel && dbRels.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            relationshipPanel.setVisible(true);
        }

        relationshipPanel.selectRelationships(dbRels);
        attributeRelationshipTab.updateActions(dbRels);
    }

    private void currentDbAttributeChanged(DbAttributeDisplayEvent e) {
        if (e.getEntity() == null)
            return;

        // update attribute selection
        DbAttributePanel attributePanel = (DbAttributePanel) attributeRelationshipTab.getSplitPane().getComponent(0);
        DbAttribute[] dbAttrs = e.getAttributes();

        if (getSelectedComponent() != attributePanel && dbAttrs.length > 0) {
            setSelectedComponent(attributeRelationshipTab);
            attributePanel.setVisible(true);
        }

        attributePanel.selectAttributes(dbAttrs);
        attributeRelationshipTab.updateActions(dbAttrs);
    }
}

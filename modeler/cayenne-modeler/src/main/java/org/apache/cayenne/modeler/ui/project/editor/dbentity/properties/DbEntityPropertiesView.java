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
package org.apache.cayenne.modeler.ui.project.editor.dbentity.properties;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.event.model.DbEntityListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.ui.action.CreateObjEntityFromDbAction;
import org.apache.cayenne.modeler.ui.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.ui.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.DbEntityCounterpartAction;
import org.apache.cayenne.modeler.ui.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.ui.action.ModelerAbstractAction;
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

public class DbEntityPropertiesView extends JPanel implements DbEntityDisplayListener, DbEntityListener {

    private final DbAttributePanel attributePanel;
    private final DbRelationshipPanel relationshipPanel;
    private final JButton editButton;
    private final JSplitPane splitPane;
    private final JToolBar toolBar;

    public DbEntityPropertiesView(ProjectController controller) {

        this.setLayout(new BorderLayout());

        editButton = new ModelerAbstractAction.CayenneToolbarButton(null, 0);

        attributePanel = new DbAttributePanel(controller, this);
        relationshipPanel = new DbRelationshipPanel(controller, this);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, attributePanel, relationshipPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);

        try {
            ComponentGeometry geometry = new ComponentGeometry(
                    this.getClass(),
                    "dbEntityAttrRelTab/splitPane/divider");

            geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, -1);
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Cannot bind divider property", ex);
        }

        add(splitPane);
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        GlobalActions globalActions = Application.getInstance().getActionManager();

        toolBar.add(globalActions.getAction(CreateAttributeAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();

        toolBar.add(globalActions.getAction(CreateObjEntityFromDbAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(DbEntitySyncAction.class).buildButton(2));
        toolBar.add(globalActions.getAction(DbEntityCounterpartAction.class).buildButton(3));
        toolBar.addSeparator();

        Icon ico = ModelerUtil.buildIcon("icon-edit.png");
        editButton.setToolTipText("Edit");
        editButton.setIcon(ico);
        editButton.setDisabledIcon(FilteredIconFactory.createDisabledIcon(ico));
        toolBar.add(editButton).setEnabled(false);

        toolBar.addSeparator();
        toolBar.add(globalActions.getAction(RemoveAttributeRelationshipAction.class).buildButton());
        toolBar.addSeparator();
        toolBar.add(globalActions.getAction(CutAttributeRelationshipAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(CopyAttributeRelationshipAction.class).buildButton(2));
        toolBar.add(globalActions.getAction(PasteAction.class).buildButton(3));

        add(toolBar, BorderLayout.NORTH);
        controller.addDbEntityDisplayListener(this);
    }

    public void updateActions(Object[] params) {
        ModelerUtil.updateActions(
                params.length,
                RemoveAttributeRelationshipAction.class,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
        if (params instanceof DbRelationship[]) {
            editButton.setEnabled(params.length > 0);
        }
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

    public DbAttributePanel getAttributePanel() {
        return attributePanel;
    }

    public DbRelationshipPanel getRelationshipPanel() {
        return relationshipPanel;
    }

    public void dbEntityChanged(DbEntityEvent e) {
        relationshipPanel.dbEntityChanged(e);
    }

    public void dbEntityAdded(DbEntityEvent e) {
        relationshipPanel.dbEntityAdded(e);
    }

    public void dbEntityRemoved(DbEntityEvent e) {
        relationshipPanel.dbEntityRemoved(e);
    }

    public void dbEntitySelected(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();
        if (entity.getDataMap().getMappedEntities(entity).isEmpty()) {
            toolBar.getComponentAtIndex(4).setEnabled(false);
            toolBar.getComponentAtIndex(5).setEnabled(false);
        } else {
            toolBar.getComponentAtIndex(4).setEnabled(true);
            toolBar.getComponentAtIndex(5).setEnabled(true);
        }
    }
}
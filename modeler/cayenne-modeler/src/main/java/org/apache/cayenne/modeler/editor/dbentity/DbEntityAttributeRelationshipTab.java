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
package org.apache.cayenne.modeler.editor.dbentity;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityFromDbAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.DbEntityCounterpartAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
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
import java.awt.event.ActionListener;

/**
 * Combines DbEntityAttributeTab and DbEntityRelationshipTab in JSplitPane.
 */
public class DbEntityAttributeRelationshipTab extends JPanel implements DbEntityDisplayListener, DbEntityListener {

    private final DbEntityAttributePanel attributePanel;
    private final DbEntityRelationshipPanel relationshipPanel;
    private final JButton editButton;
    private final JSplitPane splitPane;
    private final JToolBar toolBar;

    public DbEntityAttributeRelationshipTab(ProjectController controller) {

        this.setLayout(new BorderLayout());

        editButton = new CayenneAction.CayenneToolbarButton(null, 0);

        attributePanel = new DbEntityAttributePanel(controller, this);
        relationshipPanel = new DbEntityRelationshipPanel(controller, this);

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
        ActionManager actionManager = Application.getInstance().getActionManager();

        toolBar.add(actionManager.getAction(CreateAttributeAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();

        toolBar.add(actionManager.getAction(CreateObjEntityFromDbAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(DbEntitySyncAction.class).buildButton(2));
        toolBar.add(actionManager.getAction(DbEntityCounterpartAction.class).buildButton(3));
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

    public DbEntityAttributePanel getAttributePanel() {
        return attributePanel;
    }

    public DbEntityRelationshipPanel getRelationshipPanel() {
        return relationshipPanel;
    }

    public void dbEntityChanged(EntityEvent e) {
        relationshipPanel.dbEntityChanged(e);
    }

    public void dbEntityAdded(EntityEvent e) {
        relationshipPanel.dbEntityAdded(e);
    }

    public void dbEntityRemoved(EntityEvent e) {
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
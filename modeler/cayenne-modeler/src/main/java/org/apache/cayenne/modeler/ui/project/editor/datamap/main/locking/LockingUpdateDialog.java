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

package org.apache.cayenne.modeler.ui.project.editor.datamap.main.locking;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.project.ProjectSession;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * Modal "apply default lock type to all ObjEntities/attributes/relationships in this DataMap"
 * confirmation dialog. The default lock type is read from the DataMap; the dialog lets the user
 * pick which entity members to update.
 */
public class LockingUpdateDialog extends ProjectDialog {

    private final DataMap dataMap;

    private final JCheckBox entities;
    private final JCheckBox attributes;
    private final JCheckBox relationships;
    private final JButton cancelButton;
    private final JButton updateButton;

    public LockingUpdateDialog(ProjectSession session, Window owner, DataMap dataMap) {
        super(session, owner,
                dataMap.getDefaultLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC
                        ? "Enable Optimistic Locking" : "Disable Optimistic Locking",
                ModalityType.APPLICATION_MODAL);
        this.dataMap = dataMap;

        // check all by default until we start storing this in preferences
        this.entities = new JCheckBox("Update all Entities");
        this.entities.setSelected(true);
        this.attributes = new JCheckBox("Update all Attributes");
        this.attributes.setSelected(true);
        this.relationships = new JCheckBox("Update all Relationships");
        this.relationships.setSelected(true);

        this.cancelButton = new JButton("Cancel");
        this.updateButton = new JButton("Update");

        initLayout();
        initBindings();
    }

    private void initLayout() {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "left:max(180dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.add(entities, cc.xy(1, 1));
        builder.add(attributes, cc.xy(1, 3));
        builder.add(relationships, cc.xy(1, 5));

        getRootPane().setDefaultButton(updateButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(updateButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(builder.getPanel(), BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initBindings() {
        cancelButton.addActionListener(e -> dispose());
        updateButton.addActionListener(e -> updateAction());
    }

    private void updateAction() {
        int defaultLockType = dataMap.getDefaultLockType();
        boolean on = defaultLockType == ObjEntity.LOCK_TYPE_OPTIMISTIC;

        boolean updateEntities = entities.isSelected();
        boolean updateAttributes = attributes.isSelected();
        boolean updateRelationships = relationships.isSelected();

        for (ObjEntity entity : dataMap.getObjEntities()) {
            if (updateEntities && defaultLockType != entity.getDeclaredLockType()) {
                entity.setDeclaredLockType(defaultLockType);
                session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
            }

            if (updateAttributes) {
                for (ObjAttribute a : entity.getAttributes()) {
                    if (a.isUsedForLocking() != on) {
                        a.setUsedForLocking(on);
                        session.fireObjAttributeEvent(ObjAttributeEvent.ofChange(this, a, entity));
                    }
                }
            }

            if (updateRelationships) {
                for (ObjRelationship r : entity.getRelationships()) {
                    if (r.isUsedForLocking() != on) {
                        r.setUsedForLocking(on);
                        session.fireObjRelationshipEvent(ObjRelationshipEvent.ofChange(this, r, entity));
                    }
                }
            }
        }

        dispose();
    }
}

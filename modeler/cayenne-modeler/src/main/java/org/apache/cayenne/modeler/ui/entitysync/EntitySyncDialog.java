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

package org.apache.cayenne.modeler.ui.entitysync;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.merge.context.EntityMergeSupport;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;

/**
 * Builds an {@link EntityMergeSupport} for synchronizing ObjEntities with their parent
 * DbEntity. If any of the involved entities have meaningful FK attributes, prompts the
 * user (via this dialog) to confirm whether those should be removed; otherwise returns
 * a default merger without showing UI.
 */
public class EntitySyncDialog extends AppDialog {

    private final DbEntity dbEntity;
    private final ObjEntity objEntity;

    private final JCheckBox removeFKs;
    private final JButton updateButton;
    private final JButton cancelButton;

    private boolean cancelled;

    public EntitySyncDialog(Application app, Window owner, DbEntity dbEntity) {
        this(app, owner, dbEntity, null);
    }

    public EntitySyncDialog(Application app, Window owner, ObjEntity objEntity) {
        this(app, owner, objEntity.getDbEntity(), objEntity);
    }

    private EntitySyncDialog(Application app, Window owner, DbEntity dbEntity, ObjEntity objEntity) {
        super(app, owner, "Synchronize ObjEntity with DbEntity", ModalityType.APPLICATION_MODAL);
        this.dbEntity = dbEntity;
        this.objEntity = objEntity;

        this.removeFKs = new JCheckBox();
        this.removeFKs.setSelected(true);
        this.updateButton = new JButton("Continue");
        this.cancelButton = new JButton("Cancel");

        initLayout();
        initBindings();
    }

    /**
     * Returns a configured merger, prompting the user via the dialog only when the
     * involved entities have meaningful FK attributes. Returns null if the user cancelled.
     */
    public EntityMergeSupport createMerger() {
        Collection<ObjEntity> entities = objEntities();
        if (entities.isEmpty()) {
            return null;
        }

        ObjectNameGenerator nameGenerator = new DefaultObjectNameGenerator();

        EntityMergeSupport merger = new EntityMergeSupport(
                nameGenerator,
                NamePatternMatcher.EXCLUDE_ALL,
                true,
                false);

        for (ObjEntity entity : entities) {
            if (!merger.getMeaningfulFKs(entity).isEmpty()) {
                return confirmMeaningfulFKs(nameGenerator);
            }
        }

        return merger;
    }

    private EntityMergeSupport confirmMeaningfulFKs(ObjectNameGenerator nameGenerator) {
        pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        centerOnOwner();
        makeCloseableOnEscape();
        setVisible(true);

        return cancelled
                ? null
                : new EntityMergeSupport(nameGenerator, NamePatternMatcher.EXCLUDE_ALL, removeFKs.isSelected(), false);
    }

    private Collection<ObjEntity> objEntities() {
        return objEntity == null ? dbEntity.getDataMap().getMappedEntities(dbEntity)
                : Collections.singleton(objEntity);
    }

    private void initLayout() {
        getRootPane().setDefaultButton(updateButton);

        FormLayout layout = new FormLayout("pref, $lcgap, pref", "p, $rgap");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();
        builder.add(removeFKs, cc.xy(1, 1));
        builder.add(new JLabel("Remove Object Attributes mapped on Foreign Keys?"), cc.xy(3, 1));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(updateButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(builder.getPanel(), BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);
    }

    private void initBindings() {
        updateButton.addActionListener(e -> dispose());
        cancelButton.addActionListener(e -> {
            cancelled = true;
            dispose();
        });
    }
}

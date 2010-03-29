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

package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.util.EntityMergeSupport;

public class EntitySyncController extends CayenneController {

    protected DbEntity dbEntity;
    protected ObjEntity objEntity;
    protected EntitySyncDialog view;

    /**
     * Creates a controller for synchronizing all ObjEntities mapped to a given DbEntity.
     */
    public EntitySyncController(CayenneController parent, DbEntity dbEntity) {
        super(parent);
        this.dbEntity = dbEntity;
    }

    /**
     * Creates a controller for synchronizing a single ObjEntity with its parent DbEntity.
     */
    public EntitySyncController(CayenneController parent, ObjEntity objEntity) {
        this(parent, objEntity.getDbEntity());
        this.objEntity = objEntity;
    }

    public EntityMergeSupport createMerger() {
        Collection<ObjEntity> entities = getObjEntities();
        if (entities.isEmpty()) {
            return null;
        }

        final EntityMergeSupport merger = new EntityMergeSupport(dbEntity.getDataMap());

        // see if we need to remove meaningful attributes...
        boolean showDialog = false;
        for (ObjEntity entity : entities) {
            if (!merger.getMeaningfulFKs(entity).isEmpty()) {
                showDialog = true;
                break;
            }
        }

        return (showDialog) ? configureMerger(merger) : merger;
    }

    /**
     * Displays a nerger config dialog, returning a merger configured by the user. Returns
     * null if the dialog was canceled.
     */
    protected EntityMergeSupport configureMerger(final EntityMergeSupport merger) {

        final boolean[] cancel = new boolean[1];

        view = new EntitySyncDialog();

        view.getUpdateButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                merger.setRemoveMeaningfulFKs(view.getRemoveFKs().isSelected());
                view.dispose();
            }
        });

        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancel[0] = true;
                view.dispose();
            }
        });

        view.pack();
        view.setModal(true);
        centerView();
        makeCloseableOnEscape();
        view.setVisible(true);

        return cancel[0] ? null : merger;
    }

    @Override
    public Component getView() {
        return view;
    }

    protected Collection<ObjEntity> getObjEntities() {
        return (objEntity != null) ? Collections.singleton(objEntity) : dbEntity
                .getDataMap()
                .getMappedEntities(dbEntity);
    }

}

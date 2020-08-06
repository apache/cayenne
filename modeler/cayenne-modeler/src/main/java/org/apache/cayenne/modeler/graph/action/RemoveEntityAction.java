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
package org.apache.cayenne.modeler.graph.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.graph.GraphBuilder;
import org.apache.cayenne.modeler.undo.RemoveUndoableEdit;

/**
 * Action for removing entities from the graph
 */
public class RemoveEntityAction extends RemoveAction {
    GraphBuilder builder;
    
    public RemoveEntityAction(GraphBuilder builder) {
        super(Application.getInstance());
        this.builder = builder;
        setEnabled(true);
    }

    @Override
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);

        Entity entity = builder.getSelectedEntity();
        if (entity == null) {
            return;
        }
        
        if (entity instanceof ObjEntity) {
            if (dialog.shouldDelete("ObjEntity", entity.getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(entity.getDataMap(), (ObjEntity) entity));
                removeObjEntity(entity.getDataMap(), (ObjEntity) entity);
            }
        }
        else {
            if (dialog.shouldDelete("DbEntity", entity.getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(entity.getDataMap(), (DbEntity) entity));
                removeDbEntity(entity.getDataMap(), (DbEntity) entity);
            }
        }
    }
}

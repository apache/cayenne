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

package org.apache.cayenne.modeler.action.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

import java.awt.event.ActionEvent;

/**
 * @since 4.1
 */
public class AddSchemaAction extends TreeManipulationAction {

    private static final String ACTION_NAME = "Add Schema";
    private static final String ICON_NAME = "icon-dbi-schema.png";

    public AddSchemaAction(Application application) {
        super(ACTION_NAME, application);
        insertableNodeClass = Schema.class;
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        ReverseEngineering reverseEngineeringOldCopy = prepareElements();
        Schema newSchema = new Schema(name);
        if (canBeInserted(selectedElement)) {
            ((SchemaContainer) selectedElement.getUserObject()).addSchema(newSchema);
            selectedElement.add(new DbImportTreeNode(newSchema));
            updateSelected = true;
        } else if (canInsert()) {
            if (parentElement.isReverseEngineering()) {
                ((ReverseEngineering) parentElement.getUserObject()).addSchema(newSchema);
            } else {
                ((Catalog) parentElement.getUserObject()).addSchema(newSchema);
            }
            parentElement.add(new DbImportTreeNode(newSchema));
            updateSelected = false;
        }
        completeInserting(reverseEngineeringOldCopy);
    }
}

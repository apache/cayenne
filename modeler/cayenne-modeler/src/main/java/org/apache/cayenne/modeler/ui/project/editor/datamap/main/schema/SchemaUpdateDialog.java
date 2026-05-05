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

package org.apache.cayenne.modeler.ui.project.editor.datamap.main.schema;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datamap.defaults.DefaultsUpdateDialog;
import org.apache.cayenne.util.Util;

import java.awt.Window;

public class SchemaUpdateDialog extends DefaultsUpdateDialog {

    private static final String ALL_CONTROL = "Set/update schema for all DbEntities";
    private static final String UNINIT_CONTROL = "Do not override existing non-empty schema";

    public SchemaUpdateDialog(ProjectSession session, Window owner, DataMap dataMap) {
        super(session, owner, dataMap, "Update DbEntities Schema", ALL_CONTROL, UNINIT_CONTROL);
    }

    @Override
    protected void applyUpdate() {
        boolean doAll = isAllEntities();
        String defaultSchema = dataMap.getDefaultSchema();

        for (DbEntity entity : dataMap.getDbEntities()) {
            if (doAll || Util.isEmptyString(entity.getSchema())) {
                if (!Util.nullSafeEquals(defaultSchema, entity.getSchema())) {
                    entity.setSchema(defaultSchema);
                    session().fireDbEntityEvent(DbEntityEvent.ofChange(this, entity));
                }
            }
        }

        for (Procedure procedure : dataMap.getProcedures()) {
            if (doAll || Util.isEmptyString(procedure.getSchema())) {
                if (!Util.nullSafeEquals(defaultSchema, procedure.getSchema())) {
                    procedure.setSchema(defaultSchema);
                    session().fireProcedureEvent(ProcedureEvent.ofChange(this, procedure));
                }
            }
        }
    }
}

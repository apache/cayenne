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

package org.apache.cayenne.modeler.dialog.datamap;

import java.util.Iterator;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.Util;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;
import org.scopemvc.view.swing.SPanel;

/**
 * A controller for batch DbEntities schema update.
 * 
 */
public class SchemaUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = "cayenne.modeler.datamap.defaultprefs.schema.radio";
    public static final String UNINIT_CONTROL = "cayenne.modeler.datamap.defaultprefs.schemanull.radio";

    public SchemaUpdateController(ProjectController mediator, DataMap dataMap) {
        super(mediator, dataMap);
    }

    /**
     * Creates and runs the schema update dialog.
     */
    public void startup() {
        SPanel view = new DefaultsPreferencesDialog(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update DbEntities Schema");
        setView(view);
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(UPDATE_CONTROL)) {
            updateSchema();
        }
        else {
            super.doHandleControl(control);
        }
    }

    protected void updateSchema() {
        boolean doAll = ((DefaultsPreferencesModel) getModel()).isAllEntities();
        String defaultSchema = dataMap.getDefaultSchema();

        // set schema for DbEntities
        for (DbEntity entity : dataMap.getDbEntities()) {
            if (doAll || Util.isEmptyString(entity.getSchema())) {
                if (!Util.nullSafeEquals(defaultSchema, entity.getSchema())) {
                    entity.setSchema(defaultSchema);

                    // any way to batch events, a big change will flood the app with
                    // entity events..?
                    mediator.fireDbEntityEvent(new EntityEvent(this, entity));
                }
            }
        }

        // set schema for procedures...
        for (Procedure procedure : dataMap.getProcedures()) {
            if (doAll || Util.isEmptyString(procedure.getSchema())) {
                if (!Util.nullSafeEquals(defaultSchema, procedure.getSchema())) {
                    procedure.setSchema(defaultSchema);

                    // any way to batch events, a big change will flood the app with
                    // procedure events..?
                    mediator.fireProcedureEvent(new ProcedureEvent(this, procedure));
                }
            }
        }
        shutdown();
    }
}

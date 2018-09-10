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

import java.awt.Component;
import javax.swing.WindowConstants;

import org.apache.cayenne.configuration.event.ProcedureEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.Util;

/**
 * A controller for batch DbEntities schema update.
 * 
 */
public class SchemaUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = "Set/update schema for all DbEntities";
    public static final String UNINIT_CONTROL = "Do not override existing non-empty schema";
    
    protected DefaultsPreferencesView view;

    public SchemaUpdateController(ProjectController mediator, DataMap dataMap) {
        super(mediator, dataMap);
    }

    /**
     * Creates and runs the schema update dialog.
     */
    public void startupAction() {
        view = new DefaultsPreferencesView(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update DbEntities Schema");
        initController();
        
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }
    
    public Component getView() {
        return this.view;
    }
    
    private void initController() {
        view.getUpdateButton().addActionListener(e -> updateSchema());
        view.getCancelButton().addActionListener(e -> view.dispose());
    }

    protected void updateSchema() {
        boolean doAll = isAllEntities();
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
        view.dispose();
    }
}

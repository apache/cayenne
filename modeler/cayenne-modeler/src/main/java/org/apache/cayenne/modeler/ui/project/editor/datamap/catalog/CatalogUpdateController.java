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
package org.apache.cayenne.modeler.ui.project.editor.datamap.catalog;

import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.defaults.DefaultsPreferencesController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.defaults.DefaultsPreferencesView;
import org.apache.cayenne.util.Util;

import javax.swing.*;

public class CatalogUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = "Set/update catalog for all DbEntities";
    public static final String UNINIT_CONTROL = "Do not override existing non-empty catalog";

    private DefaultsPreferencesView view;

    public CatalogUpdateController(ProjectController controller, DataMap dataMap) {
        super(controller, dataMap);
    }

    public void startupAction() {
        view = new DefaultsPreferencesView(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update DbEntities Catalog");
        view.getUpdateButton().addActionListener(e -> updateCatalog());
        view.getCancelButton().addActionListener(e -> view.dispose());

        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    @Override
    public DefaultsPreferencesView getView() {
        return this.view;
    }

    private void updateCatalog() {
        boolean doAll = isAllEntities();
        String defaultCatalog = dataMap.getDefaultCatalog();

        // set catalog for DbEntities
        for (DbEntity entity : dataMap.getDbEntities()) {
            if (doAll || Util.isEmptyString(entity.getCatalog())) {
                if (!Util.nullSafeEquals(defaultCatalog, entity.getCatalog())) {
                    entity.setCatalog(defaultCatalog);

                    // any way to batch events, a big change will flood the app
                    // with entity events..?
                    parent.fireDbEntityEvent(new EntityEvent(this, entity));
                }
            }
        }

        // set catalog for procedures...
        for (Procedure procedure : dataMap.getProcedures()) {
            if (doAll || Util.isEmptyString(procedure.getCatalog())) {
                if (!Util.nullSafeEquals(defaultCatalog, procedure.getCatalog())) {
                    procedure.setCatalog(defaultCatalog);

                    // any way to batch events, a big change will flood the app
                    // with procedure events..?
                    parent.fireProcedureEvent(new ProcedureEvent(this, procedure));
                }
            }
        }
        view.dispose();
    }

}

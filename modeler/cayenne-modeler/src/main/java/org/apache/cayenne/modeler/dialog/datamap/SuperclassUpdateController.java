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

package org.apache.cayenne.modeler.dialog.datamap;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.awt.*;

public class SuperclassUpdateController extends DefaultsPreferencesController {

    private static final String ALL_CONTROL = "Set/update superclass for all ObjEntities";
    private static final String UNINIT_CONTROL = "Do not override existing non-empty superclasses";

    private DefaultsPreferencesView view;

    public SuperclassUpdateController(ProjectController mediator, DataMap dataMap) {
        super(mediator, dataMap);
    }

    /**
     * Creates and runs superclass update dialog.
     */
    public void startupAction() {
        view = new DefaultsPreferencesView(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update Persistent objects Superclass");
        view.getCancelButton().addActionListener(e -> view.dispose());
        view.getUpdateButton().addActionListener(e -> updateSuperclass());

        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    @Override
    public Component getView() {
        return this.view;
    }

    protected void updateSuperclass() {
        boolean doAll = isAllEntities();
        String defaultSuperclass = dataMap.getDefaultSuperclass();

        dataMap.getObjEntities().stream()
                .sorted(Comparators.getDataMapChildrenComparator()).forEach(entity -> {
                    if (doAll || Util.isEmptyString(getSuperClassName(entity))) {
                        if (!Util.nullSafeEquals(defaultSuperclass, getSuperClassName(entity))) {
                            setSuperClassName(entity, defaultSuperclass);

                            // any way to batch events, a big change will flood the app with
                            // entity events..?
                            mediator.fireDbEntityEvent(new EntityEvent(this, entity));
                        }
                    }
                });

        view.dispose();
    }

    private String getSuperClassName(ObjEntity entity) {
        return entity.getSuperClassName();
    }

    private void setSuperClassName(ObjEntity entity, String superClassName) {
        entity.setSuperClassName(superClassName);
    }
}

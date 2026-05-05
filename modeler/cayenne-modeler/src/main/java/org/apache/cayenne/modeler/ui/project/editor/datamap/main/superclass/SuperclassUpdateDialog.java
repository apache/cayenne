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

package org.apache.cayenne.modeler.ui.project.editor.datamap.main.superclass;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datamap.defaults.DefaultsUpdateDialog;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.util.Util;

import java.awt.Window;

public class SuperclassUpdateDialog extends DefaultsUpdateDialog {

    private static final String ALL_CONTROL = "Set/update superclass for all ObjEntities";
    private static final String UNINIT_CONTROL = "Do not override existing non-empty superclasses";

    public SuperclassUpdateDialog(ProjectSession session, Window owner, DataMap dataMap) {
        super(session, owner, dataMap, "Update Persistent objects Superclass", ALL_CONTROL, UNINIT_CONTROL);
    }

    @Override
    protected void applyUpdate() {
        boolean doAll = isAllEntities();
        String defaultSuperclass = dataMap.getDefaultSuperclass();

        dataMap.getObjEntities().stream()
                .sorted(Comparators.forDataMapChildren()).forEach(entity -> {
                    if (doAll || Util.isEmptyString(entity.getSuperClassName())) {
                        if (!Util.nullSafeEquals(defaultSuperclass, entity.getSuperClassName())) {
                            entity.setSuperClassName(defaultSuperclass);
                            // any way to batch events, a big change will flood the app with entity events?
                            session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
                        }
                    }
                });
    }
}

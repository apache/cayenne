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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.Util;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;
import org.scopemvc.view.swing.SPanel;

/**
 * @author Andrei Adamchik
 */
public class SuperclassUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = "cayenne.modeler.datamap.defaultprefs.superclass.radio";
    public static final String UNINIT_CONTROL = "cayenne.modeler.datamap.defaultprefs.superclassnull.radio";

    public SuperclassUpdateController(ProjectController mediator, DataMap dataMap) {
        super(mediator, dataMap);
    }

    /**
     * Creates and runs superclass update dialog.
     */
    public void startup() {
        SPanel view = new DefaultsPreferencesDialog(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update DataObjects Superclass");
        setView(view);
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(UPDATE_CONTROL)) {
            updateSuperclass();
        }
        else {
            super.doHandleControl(control);
        }
    }

    protected void updateSuperclass() {
        boolean doAll = ((DefaultsPreferencesModel) getModel()).isAllEntities();
        String defaultSuperclass = dataMap.getDefaultSuperclass();

        Iterator it = dataMap.getObjEntities().iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();
            if (doAll || Util.isEmptyString(entity.getSuperClassName())) {
                if (!Util.nullSafeEquals(defaultSuperclass, entity.getSuperClassName())) {
                    entity.setSuperClassName(defaultSuperclass);

                    // any way to batch events, a big change will flood the app with
                    // entity events..?
                    mediator.fireDbEntityEvent(new EntityEvent(this, entity));
                }
            }
        }

        shutdown();
    }

}

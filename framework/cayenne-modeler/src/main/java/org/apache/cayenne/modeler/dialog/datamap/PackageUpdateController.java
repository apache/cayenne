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
 * @author Andrus Adamchik
 */
public class PackageUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = "cayenne.modeler.datamap.defaultprefs.package.radio";
    public static final String UNINIT_CONTROL = "cayenne.modeler.datamap.defaultprefs.packagenull.radio";

    protected boolean clientUpdate;

    public PackageUpdateController(ProjectController mediator, DataMap dataMap,
            boolean clientUpdate) {
        super(mediator, dataMap);
        this.clientUpdate = clientUpdate;
    }

    /**
     * Creates and runs the package update dialog.
     */
    public void startup() {
        SPanel view = new DefaultsPreferencesDialog(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update ObjEntities Java Package");
        setView(view);
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(UPDATE_CONTROL)) {
            updatePackage();
        }
        else {
            super.doHandleControl(control);
        }
    }

    protected void updatePackage() {
        boolean doAll = ((DefaultsPreferencesModel) getModel()).isAllEntities();
        String defaultPackage = getDefaultPackage();
        if (Util.isEmptyString(defaultPackage)) {
            defaultPackage = "";
        }
        else if (!defaultPackage.endsWith(".")) {
            defaultPackage = defaultPackage + '.';
        }

        Iterator it = dataMap.getObjEntities().iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();
            String oldName = getClassName(entity);
            
            if (doAll || Util.isEmptyString(oldName) || oldName.indexOf('.') < 0) {
                String className = extractClassName(Util.isEmptyString(oldName) ? entity
                        .getName() : oldName);
                setClassName(entity, defaultPackage + className);
            }
        }

        shutdown();
    }

    protected String extractClassName(String name) {
        if (name == null) {
            return "";
        }

        int dot = name.lastIndexOf('.');
        return (dot < 0) ? name : (dot + 1 < name.length())
                ? name.substring(dot + 1)
                : "";
    }

    protected String getDefaultPackage() {
        return clientUpdate ? dataMap.getDefaultClientPackage() : dataMap
                .getDefaultPackage();
    }

    protected String getClassName(ObjEntity entity) {
        return clientUpdate ? entity.getClientClassName() : entity.getClassName();
    }

    protected void setClassName(ObjEntity entity, String newName) {
        if (!Util.nullSafeEquals(newName, getClassName(entity))) {

            if (clientUpdate) {
                entity.setClientClassName(newName);
            }
            else {
                entity.setClassName(newName);
            }

            mediator.fireObjEntityEvent(new EntityEvent(this, entity));
        }
    }
}

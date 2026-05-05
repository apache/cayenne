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

package org.apache.cayenne.modeler.ui.project.editor.datamap.main.pkg;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datamap.defaults.DefaultsUpdateDialog;
import org.apache.cayenne.util.Util;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PackageUpdateDialog extends DefaultsUpdateDialog {

    private static final String ALL_CONTROL =
            "Set/update package for all ObjEntities and Embeddables (create default class names if missing)";
    private static final String UNINIT_CONTROL = "Do not override class names with packages";

    public PackageUpdateDialog(ProjectSession session, Window owner, DataMap dataMap) {
        super(session, owner, dataMap, "Update ObjEntities and Embeddables Java Package", ALL_CONTROL, UNINIT_CONTROL);
    }

    @Override
    protected void applyUpdate() {
        boolean doAll = isAllEntities();

        Map<String, String> oldNameEmbeddableToNewName = new HashMap<>();

        // Local copy to avoid ConcurrentModificationException
        Collection<Embeddable> embeddables = new ArrayList<>(dataMap.getEmbeddables());
        for (Embeddable embeddable : embeddables) {
            String oldName = embeddable.getClassName();
            String[] tokens = oldName.split("\\.");
            String className = tokens[tokens.length - 1];

            if (doAll || Util.isEmptyString(oldName) || oldName.indexOf('.') < 0) {
                EmbeddableEvent e = EmbeddableEvent.ofChange(this, embeddable, embeddable.getClassName());
                String newClassName = dataMap.getNameWithDefaultPackage(className);
                oldNameEmbeddableToNewName.put(oldName, newClassName);
                embeddable.setClassName(newClassName);
                session().fireEmbeddableEvent(e, session().getSelectedDataMap());
            }
        }

        for (ObjEntity entity : dataMap.getObjEntities()) {
            String oldName = entity.getClassName();

            if (doAll || Util.isEmptyString(oldName) || oldName.indexOf('.') < 0) {
                String className = extractClassName(Util.isEmptyString(oldName) ? entity.getName() : oldName);
                String newName = dataMap.getNameWithDefaultPackage(className);
                if (!Util.nullSafeEquals(newName, entity.getClassName())) {
                    entity.setClassName(newName);
                    session().fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
                }
            }

            for (ObjAttribute attribute : entity.getAttributes()) {
                if (attribute instanceof EmbeddedAttribute
                        && !oldNameEmbeddableToNewName.isEmpty()
                        && oldNameEmbeddableToNewName.containsKey(attribute.getType())) {
                    attribute.setType(oldNameEmbeddableToNewName.get(attribute.getType()));
                    session().fireObjAttributeEvent(ObjAttributeEvent.ofChange(this, attribute, entity));
                }
            }
        }
    }

    private static String extractClassName(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return (dot < 0)
                ? name
                : (dot + 1 < name.length()) ? name.substring(dot + 1) : "";
    }
}

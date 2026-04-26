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
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.defaults.DefaultsPreferencesController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.defaults.DefaultsPreferencesView;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class PackageUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = 
            "Set/update package for all ObjEntities and Embeddables (create default class names if missing)";
    public static final String UNINIT_CONTROL = "Do not override class names with packages";
    
    protected DefaultsPreferencesView view;

    public PackageUpdateController(ProjectController controller, DataMap dataMap) {
        super(controller, dataMap);
    }

    /**
     * Creates and runs the package update dialog.
     */
    public void startupAction() {
        view = new DefaultsPreferencesView(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update ObjEntities and Embeddables Java Package");
        view.getCancelButton().addActionListener(e -> view.dispose());
        view.getUpdateButton().addActionListener(e -> updatePackage());
        
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

    private void updatePackage() {
        boolean doAll = isAllEntities();

        Map<String, String> oldNameEmbeddableToNewName = new HashMap<>();

        // Create local copy to escape ConcurrentModificationException
        Collection<Embeddable> embeddables = new ArrayList<>(dataMap.getEmbeddables());
        for (Embeddable embeddable : embeddables) {
            String oldName = embeddable.getClassName();

            String[] tokens = oldName.split("\\.");
            String className = tokens[tokens.length-1];

            if (doAll || Util.isEmptyString(oldName) || oldName.indexOf('.') < 0) {
                EmbeddableEvent e = EmbeddableEvent.ofChange(this, embeddable, embeddable.getClassName());
                String newClassName = getNameWithDefaultPackage(className);
                oldNameEmbeddableToNewName.put(oldName, newClassName);
                embeddable.setClassName(newClassName);
                parent.fireEmbeddableEvent(e, parent.getSelectedDataMap());
            }
        }

        for (ObjEntity entity : dataMap.getObjEntities()) {
            String oldName = getClassName(entity);

            if (doAll || Util.isEmptyString(oldName) || oldName.indexOf('.') < 0) {
                String className = extractClassName(Util.isEmptyString(oldName) ? entity.getName() : oldName);
                setClassName(entity, getNameWithDefaultPackage(className));
            }

            for(ObjAttribute attribute: entity.getAttributes()){
                if(attribute instanceof EmbeddedAttribute){
                    if(!oldNameEmbeddableToNewName.isEmpty() && oldNameEmbeddableToNewName.containsKey(attribute.getType())){
                        attribute.setType(oldNameEmbeddableToNewName.get(attribute.getType()));
                        ObjAttributeEvent ev = ObjAttributeEvent.ofChange(this, attribute, entity);
                        parent.fireObjAttributeEvent(ev);
                    }
                }
            }
        }

        view.dispose();
    }

    protected String extractClassName(String name) {
        if (name == null) {
            return "";
        }

        int dot = name.lastIndexOf('.');
        return (dot < 0)
                ? name
                : (dot + 1 < name.length()) ? name.substring(dot + 1) : "";
    }

    protected String getNameWithDefaultPackage(String name) {
        return dataMap.getNameWithDefaultPackage(name);
    }

    protected String getClassName(ObjEntity entity) {
        return entity.getClassName();
    }

    protected void setClassName(ObjEntity entity, String newName) {
        if (!Util.nullSafeEquals(newName, getClassName(entity))) {
            entity.setClassName(newName);
            parent.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
        }
    }
}

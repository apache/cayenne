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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.WindowConstants;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.Util;

/**
 */
public class PackageUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = 
            "Set/update package for all ObjEntities and Embeddables (create default class names if missing)";
    public static final String UNINIT_CONTROL = "Do not override class names with packages";

    protected boolean clientUpdate;
    
    protected DefaultsPreferencesView view;

    public PackageUpdateController(ProjectController mediator, DataMap dataMap,
            boolean clientUpdate) {
        super(mediator, dataMap);
        this.clientUpdate = clientUpdate;
    }

    /**
     * Creates and runs the package update dialog.
     */
    public void startupAction() {
        view = new DefaultsPreferencesView(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update ObjEntities and Embeddables Java Package");
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
        view.getCancelButton().addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent arg0) {
                view.dispose();
            }
        });
        view.getUpdateButton().addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent arg0) {
                updatePackage();
            }
        });
    }

    protected void updatePackage() {
        boolean doAll = isAllEntities();

        Map<String, String> oldNameEmbeddableToNewName = new HashMap<>();

        // Create local copy to escape ConcurrentModificationException
        Collection<Embeddable> embeddables = new ArrayList<>(dataMap.getEmbeddables());
        for (Embeddable embeddable : embeddables) {
            String oldName = embeddable.getClassName();

            Pattern p = Pattern.compile("[.]");
            String[] tokens = p.split(oldName);
            String className = tokens[tokens.length-1];

            if (doAll || Util.isEmptyString(oldName) || oldName.indexOf('.') < 0) {
                EmbeddableEvent e = new EmbeddableEvent(this, embeddable, embeddable.getClassName());
                String newClassName = getNameWithDefaultPackage(className);
                oldNameEmbeddableToNewName.put(oldName, newClassName);
                embeddable.setClassName(newClassName);
                mediator.fireEmbeddableEvent(e, mediator.getCurrentDataMap());
            }
        }

        for (ObjEntity entity : dataMap.getObjEntities()) {
            String oldName = getClassName(entity);

            if (doAll || Util.isEmptyString(oldName) || oldName.indexOf('.') < 0) {
                String className = extractClassName(Util.isEmptyString(oldName) ? entity
                        .getName() : oldName);
                setClassName(entity, getNameWithDefaultPackage(className));
            }

            for(ObjAttribute attribute: entity.getAttributes()){
                if(attribute instanceof EmbeddedAttribute){
                    if(oldNameEmbeddableToNewName.size()>0 && oldNameEmbeddableToNewName.containsKey(attribute.getType())){
                        attribute.setType(oldNameEmbeddableToNewName.get(attribute.getType()));
                        AttributeEvent ev = new AttributeEvent(this, attribute, entity);
                        mediator.fireObjAttributeEvent(ev);
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
        return (dot < 0) ? name : (dot + 1 < name.length())
                ? name.substring(dot + 1)
                : "";
    }

    protected String getNameWithDefaultPackage(String name) {
        if (clientUpdate) {
            return dataMap.getNameWithDefaultClientPackage(name);
        } else {
            return dataMap.getNameWithDefaultPackage(name);
        }
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

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


package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.Component;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.util.CayenneController;

/**
 */
public class ClassNameUpdater extends CayenneController {

    protected ClassNameUpdaterView view;
    protected ObjEntity entity;
    protected boolean updatePerformed;

    public ClassNameUpdater(CayenneController parent, ObjEntity entity) {
        super(parent);

        this.entity = entity;

        // don't init view here... we may simply skip update if there is nothing to do
    }

    /**
     * Executes entity class name update. Returns true if entity was changed, false otherwise.
     */
    public boolean doNameUpdate() {
        this.view = null;
        this.updatePerformed = false;

        boolean askForUpdate = true;

        String oldName = entity.getClassName();
        String suggestedName = suggestedClassName();

        if (oldName == null || oldName.length() == 0) {
            // generic entity...
            askForUpdate = false;
        } else if (suggestedName == null || suggestedName.equals(oldName)) {
            askForUpdate = false;
        } else if (oldName.contains("UntitledObjEntity")) {
            // update without user interaction
            entity.setClassName(suggestedName);
            updatePerformed = true;
            askForUpdate = false;
        }

        if (askForUpdate) {
            // start dialog
            view = new ClassNameUpdaterView();
            view.getClassName()
                    .setText("Update class name to " + suggestedName + " to match current entity name?");

            initBindings(suggestedName);

            view.pack();
            view.setModal(true);
            centerView();
            makeCloseableOnEscape();
            view.setVisible(true);
        }

        return this.updatePerformed;
    }

    private String suggestedClassName() {
        String pkg = entity.getDataMap() == null ? null : entity.getDataMap().getDefaultPackage();
        return suggestedClassName(entity.getName(), pkg, entity.getClassName());
    }

    /**
     * Suggests a new class name based on new entity name and current selections.
     */
    private static String suggestedClassName(String entityName, String suggestedPackage, String oldClassName) {

        if (entityName == null || entityName.trim().isEmpty()) {
            return null;
        }

        // build suggested package...
        String pkg = suggestedPackage;
        if (oldClassName != null && oldClassName.lastIndexOf('.') > 0) {
            pkg = oldClassName.substring(0, oldClassName.lastIndexOf('.'));
        }

        // build suggested class name
        int lastDotIndex = entityName.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < entityName.length() - 1) {
            entityName = entityName.substring(lastDotIndex + 1);
        }

        return DataMap.getNameWithPackage(pkg, entityName);
    }

    protected void initBindings(final String suggestedName) {

        view.getUpdateButton().addActionListener(e -> {
            entity.setClassName(suggestedName);
            updatePerformed = true;
            view.dispose();
        });

        view.getCancelButton().addActionListener(e -> view.dispose());
    }

    public Component getView() {
        return view;
    }
}

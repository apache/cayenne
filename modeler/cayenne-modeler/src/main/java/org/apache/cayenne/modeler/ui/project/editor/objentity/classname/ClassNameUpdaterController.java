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


package org.apache.cayenne.modeler.ui.project.editor.objentity.classname;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.mvc.RootController;

import javax.swing.JOptionPane;
import java.awt.Component;

public class ClassNameUpdaterController extends ChildController<RootController> {

    private static final String UPDATE = "Update";
    private static final String CANCEL = "Cancel";

    private final ObjEntity entity;

    public ClassNameUpdaterController(RootController parent, ObjEntity entity) {
        super(parent);
        this.entity = entity;
    }

    /**
     * Executes entity class name update. Returns true if entity was changed, false otherwise.
     */
    public boolean doNameUpdate() {
        String oldName = entity.getClassName();
        String suggestedName = suggestedClassName();

        if (oldName == null || oldName.isEmpty()) {
            // generic entity...
            return false;
        }
        if (suggestedName == null || suggestedName.equals(oldName)) {
            return false;
        }
        if (oldName.contains("UntitledObjEntity")) {
            // update without user interaction
            entity.setClassName(suggestedName);
            return true;
        }

        JOptionPane pane = new JOptionPane(
                "Update class name to '" + suggestedName + "' to match the current entity name?",
                JOptionPane.QUESTION_MESSAGE);
        pane.setOptions(new Object[]{UPDATE, CANCEL});
        pane.setInitialValue(UPDATE);

        pane.createDialog(parent.getView(), "Update Class Name").setVisible(true);

        if (UPDATE.equals(pane.getValue())) {
            entity.setClassName(suggestedName);
            return true;
        }
        return false;
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

    @Override
    public Component getView() {
        return null;
    }
}

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


package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
     * Executes entity class and client class name update. Returns true if entity was
     * changed, false otherwise.
     */
    public boolean doNameUpdate() {
        this.view = null;
        this.updatePerformed = false;

        boolean askForServerUpdate = true;
        boolean askForClientUpdate = true;

        String oldServerName = entity.getClassName();
        String suggestedServerName = suggestedServerClassName();

        if (oldServerName == null || oldServerName.length() == 0) {
            // generic entity...
            askForServerUpdate = false;
        }
        else if (suggestedServerName == null || suggestedServerName.equals(oldServerName)) {
            askForServerUpdate = false;
        }
        else if (oldServerName.contains("UntitledObjEntity")) {

            // update without user interaction
            entity.setClassName(suggestedServerName);

            updatePerformed = true;
            askForServerUpdate = false;
        }

        String suggestedClientName = suggestedClientClassName();
        String oldClientName = entity.getClientClassName();
        if (suggestedClientName == null || suggestedClientName.equals(oldClientName)) {
            askForClientUpdate = false;
        }
        else if (oldClientName == null
                || oldClientName.length() == 0
                || oldClientName.contains("UntitledObjEntity")) {

            // update without user interaction
            entity.setClientClassName(suggestedClientName);

            updatePerformed = true;
            askForClientUpdate = false;
        }

        if (askForClientUpdate || askForServerUpdate) {
            // start dialog
            view = new ClassNameUpdaterView();

            if (askForServerUpdate) {
                view.getServerClass().setVisible(true);
                view.getServerClass().setSelected(true);
                view.getServerClass().setText(
                        "Change Class Name to '" + suggestedServerName + "'");
            }

            if (askForClientUpdate) {
                view.getClientClass().setVisible(true);
                view.getClientClass().setSelected(true);
                view.getClientClass().setText(
                        "Change Client Class Name to '" + suggestedClientName + "'");
            }

            initBindings(suggestedServerName, suggestedClientName);

            view.pack();
            view.setModal(true);
            centerView();
            makeCloseableOnEscape();
            view.setVisible(true);
        }

        return this.updatePerformed;
    }

    private String suggestedServerClassName() {
        String pkg = entity.getDataMap() != null ? entity
                .getDataMap()
                .getDefaultPackage() : null;
        return suggestedClassName(entity.getName(), pkg, entity.getClassName());
    }

    private String suggestedClientClassName() {
        // do not updated client class name if it is not allowed
        if (!entity.isClientAllowed()) {
            return null;
        }

        String pkg = entity.getDataMap() != null ? entity
                .getDataMap()
                .getDefaultClientPackage() : null;
        return suggestedClassName(entity.getName(), pkg, entity.getClientClassName());
    }

    /**
     * Suggests a new class name based on new entity name and current selections.
     */
    private String suggestedClassName(
            String entityName,
            String suggestedPackage,
            String oldClassName) {

        if (entityName == null || entityName.trim().length() == 0) {
            return null;
        }

        // build suggested package...
        String pkg = suggestedPackage;
        if (oldClassName != null && oldClassName.lastIndexOf('.') > 0) {
            pkg = oldClassName.substring(0, oldClassName.lastIndexOf('.'));
        }

        if (pkg == null) {
            pkg = "";
        }
        else {
            pkg = pkg + '.';
        }

        // build suggested class name
        int dot = entityName.lastIndexOf('.');
        if (dot >= 0 && dot < entityName.length() - 1) {
            entityName = entityName.substring(dot + 1);
        }

        return pkg + entityName;
    }

    protected void initBindings(
            final String suggestedServerName,
            final String suggestedClientName) {

        view.getUpdateButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (view.getClientClass().isSelected()) {
                    entity.setClientClassName(suggestedClientName);
                    updatePerformed = true;
                }

                if (view.getServerClass().isSelected()) {
                    entity.setClassName(suggestedServerName);
                    updatePerformed = true;
                }

                view.dispose();
            }
        });

        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.dispose();
            }
        });
    }

    public Component getView() {
        return view;
    }
}

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
package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.apache.cayenne.map.EntityListener;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.EntityListenerEvent;
import org.apache.cayenne.modeler.undo.CreateEntityListenerUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * Action class for creating entity listeners on an ObjEntity
 * 
 * @version 1.0 Oct 30, 2007
 */
public class CreateObjEntityListenerAction extends CayenneAction {

    /**
     * unique action name
     */
    private static final String CREATE_ENTITY_LISTENER = "Create objentity entity listener";

    /**
     * Constructor.
     * 
     * @param application Application instance
     */
    public CreateObjEntityListenerAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * Constructor for extending classes.
     * 
     * @param actionName unique action name
     * @param application Application instance
     */
    protected CreateObjEntityListenerAction(String actionName, Application application) {
        super(actionName, application);
    }

    /**
     * @return unique action name
     */
    public static String getActionName() {
        return CREATE_ENTITY_LISTENER;
    }

    /**
     * @return icon file name for button
     */
    public String getIconName() {
        return "icon-create-listener.gif";
    }

    /**
     * checks whether the new name of listener class already exists
     * 
     * @param className entered class name
     * @return true or false
     */
    protected boolean isListenerClassAlreadyExists(String className) {
        return getProjectController().getCurrentObjEntity().getEntityListener(className) != null;
    }

    /**
     * base entity listenre creation logic
     * 
     * @param e event
     */
    public void performAction(ActionEvent e) {
        String listenerClass = JOptionPane
                .showInputDialog("Please enter listener class:");
        if (listenerClass != null && listenerClass.trim().length() > 0) {
            if (isListenerClassAlreadyExists(listenerClass)) {
                JOptionPane.showMessageDialog(
                        null,
                        "Listener class already exists.",
                        "Error creating entity listener",
                        JOptionPane.ERROR_MESSAGE);
            }
            else {
                ObjEntity objEntity = getProjectController().getCurrentObjEntity();
                EntityListener listener = new EntityListener(listenerClass);
                createEntityListener(objEntity, listener);
                application.getUndoManager().addEdit(
                        new CreateEntityListenerUndoableEdit(objEntity, listener));
            }
        }
    }

    public void createEntityListener(ObjEntity objEntity, EntityListener listener) {
        objEntity.addEntityListener(listener);

        getProjectController().fireEntityListenerEvent(
                new EntityListenerEvent(CreateObjEntityListenerAction.this, listener
                        .getClassName(), listener.getClassName(), MapEvent.ADD));
    }
}

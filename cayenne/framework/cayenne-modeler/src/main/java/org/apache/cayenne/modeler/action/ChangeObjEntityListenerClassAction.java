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

import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.EntityListenerEvent;
import org.apache.cayenne.modeler.util.CayenneAction;


/**
 * Action class for renaming entity listener class for ObjEntity
 *
 * @version 1.0 Oct 30, 2007
 */
public class ChangeObjEntityListenerClassAction extends CayenneAction {
    /**
     * unique action name
     */
    private static final String ACTION_NAME = "Change objentity entity listener class";


    /**
     * Constructor.
     *
     * @param application Application instance
     */
    public ChangeObjEntityListenerClassAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * Constructor for extending classes
     *
     * @param name unique action name
     * @param application Application instance
     */
    protected ChangeObjEntityListenerClassAction(String name, Application application) {
        super(name, application);
    }

    /**
     * @return unique action name
     */
    public static String getActionName() {
        return ACTION_NAME;
    }

    /**
     * @return icon file name for button
     */
    public String getIconName() {
        return "icon-change-listener.gif";
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
     * change entity listener class
     * @param prevListenerClass previous class name
     * @param newListenerClass new class name
     */
    protected void renameEntityListener(String prevListenerClass, String newListenerClass) {
        getProjectController().getCurrentObjEntity().getEntityListener(prevListenerClass).setClassName(newListenerClass);
    }

    /**
     * base entity listener class renaming logic
     * @param e event
     */
    public void performAction(ActionEvent e) {
        String currentListenerClass =  getProjectController().getCurrentListenerClass();
        String newListenerClass = JOptionPane.showInputDialog(
                "Please enter listener class:",
                currentListenerClass);
        if (newListenerClass != null && newListenerClass.trim().length() > 0) {
            if (isListenerClassAlreadyExists(newListenerClass)) {
                JOptionPane.showMessageDialog(
                        null,
                        "Listener class already exists.",
                        "Error creating entity listener",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            else {
                renameEntityListener(currentListenerClass, newListenerClass);
                getProjectController().fireEntityListenerEvent(
                        new EntityListenerEvent(
                                e.getSource(),
                                currentListenerClass,
                                newListenerClass,
                                MapEvent.ADD
                        )
                );
            }
        }
    }
}


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

import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.project.ProjectPath;

/**
 * Action for cutting procedure parameter(s)
 */
public class CutProcedureParameterAction extends CutAction implements MultipleObjectsAction {
    private final static String ACTION_NAME = "Cut Procedure Parameter";
    
    /**
     * Name of action if multiple attrs are selected
     */
    private final static String ACTION_NAME_MULTIPLE = "Cut Procedure Parameters";

    public static String getActionName() {
        return ACTION_NAME;
    }
    
    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }

    public CutProcedureParameterAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable
     * attribute.
     */
    @Override
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.getObject() instanceof ProcedureParameter;
    }
    
    /**
     * Performs cutting of items
     */
    @Override
    public void performAction(ActionEvent e) {
        application.getAction(CopyProcedureParameterAction.getActionName()).performAction(e);
        ((RemoveAction) application.getAction(RemoveProcedureParameterAction.getActionName()))
            .performAction(e, false);
    }
}

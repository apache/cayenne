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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.LogConsole;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;

/**
 * Action for opening log console window (See CAY-366)
 */
public class ShowLogConsoleAction extends CayenneAction {
    /**
     * Readable name for this action
     */
    public static final String getActionName() {
        return "Show log console";
    }
    
    /**
     * Constructor for ShowLogConsoleAction.
     */
    public ShowLogConsoleAction(Application application) {
        super(getActionName(), application);
    }
    
    @Override
    public void performAction(ActionEvent e) {
        LogConsole.getInstance().toggle();
    }

}

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
import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.dialog.LogConsole;
import org.apache.cayenne.project.ProjectPath;

import java.awt.event.ActionEvent;

/**
 */
public class ExitAction extends ProjectAction {

    public static String getActionName() {
    	return "Exit";
    }

    /**
     * Constructor for ExitAction.
     */
    public ExitAction(Application application) {
        super(getActionName(), application);
    }

    public void performAction(ActionEvent e) {
        exit();
    }

    public void exit() {
        if (!checkSaveOnClose()) {
            return;
        }

		// write prefs to persistent store
		ModelerPreferences.getPreferences().storePreferences();
		
		//stop logging before JVM shutdown to prevent hanging
		LogConsole.getInstance().stopLogging();

		// goodbye
        System.exit(0);
    }

    /**
    * Always returns true.
    */
    public boolean enableForPath(ProjectPath path) {
        return true;
    }
}

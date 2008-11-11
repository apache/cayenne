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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.AboutDialog;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 */
public class AboutAction extends CayenneAction {

    public static final String getActionName() {
        return "About CayenneModeler";
    }

    /**
     * Constructor for AboutMenuAction.
     * @param name
     */
    public AboutAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * Displays About dialog.
     */
    public void performAction(ActionEvent e) {
        showAboutDialog();
    }

    public void showAboutDialog() {
        new AboutDialog();
    }
}

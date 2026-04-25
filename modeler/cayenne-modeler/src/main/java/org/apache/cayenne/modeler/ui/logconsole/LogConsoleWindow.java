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
package org.apache.cayenne.modeler.ui.logconsole;

import org.apache.cayenne.modeler.swing.dialog.CayenneDialog;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * LogConsoleWindow is used to show log in a separate dialog
 */
public class LogConsoleWindow extends CayenneDialog {

    /**
     * Constructs a new log console window
     */
    public LogConsoleWindow(LogConsoleController controller) {
        super(controller.getApplication().getFrameController().getView());

        setTitle("Cayenne Modeler Console");

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                controller.setConsoleProperty(LogConsoleController.SHOW_CONSOLE_PROPERTY, false);
                controller.getApplication().getFrameController().getView().updateLogConsoleMenu();
            }
        });
    }
}

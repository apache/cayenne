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

package org.apache.cayenne.modeler.dialog;

import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * A dialog to show if cayenne.xml was renamed or deleted by other program
 */
public class FileDeletedDialog {

    private static final String SAVE = "Save Changes";
    private static final String CLOSE = "Close Project";
    private static final String CANCEL = "Cancel";

    private static final String[] OPTIONS = new String[] {
            SAVE, CLOSE, CANCEL
    };

    protected Component parent;
    protected String result = CANCEL;

    public FileDeletedDialog(Component parent) {
        this.parent = parent;
    }

    public void show() {
        JOptionPane pane = new JOptionPane(
                "One or more project files were deleted or renamed. "
                        + "Do you want to save the changes or close the project?",
                JOptionPane.QUESTION_MESSAGE);
        pane.setOptions(OPTIONS);

        JDialog dialog = pane.createDialog(parent, "File deleted");
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        // need to do an if..else chain, since
        // sometimes values are unexpected
        if (SAVE.equals(selectedValue)) {
            result = SAVE;
        }
        else if (CLOSE.equals(selectedValue)) {
            result = CLOSE;
        }
        else {
            result = CANCEL;
        }
    }

    public boolean shouldSave() {
        return SAVE.equals(result);
    }

    public boolean shouldClose() {
        return CLOSE.equals(result);
    }

    public boolean shouldCancel() {
        return result == null || CANCEL.equals(result);
    }
}

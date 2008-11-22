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
 */
public class UnsavedChangesDialog {

    private static final String SAVE_AND_CLOSE = "Save Changes";
    private static final String CLOSE_WITHOUT_SAVE = "Discard Changes";
    private static final String CANCEL = "Cancel";

    private static final String[] OPTIONS = new String[] {
            SAVE_AND_CLOSE, CLOSE_WITHOUT_SAVE, CANCEL
    };

    protected Component parent;
    protected String result = CANCEL;

    public UnsavedChangesDialog(Component parent) {
        this.parent = parent;
    }

    public void show() {
        JOptionPane pane = new JOptionPane(
                "You have unsaved changes. Do you want to save them?",
                JOptionPane.QUESTION_MESSAGE);
        pane.setOptions(OPTIONS);

        JDialog dialog = pane.createDialog(parent, "Unsaved Changes");
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        // need to do an if..else chain, since
        // sometimes values are unexpected
        if (SAVE_AND_CLOSE.equals(selectedValue)) {
            result = SAVE_AND_CLOSE;
        }
        else if (CLOSE_WITHOUT_SAVE.equals(selectedValue)) {
            result = CLOSE_WITHOUT_SAVE;
        }
        else {
            result = CANCEL;
        }
    }

    public boolean shouldSave() {
        return SAVE_AND_CLOSE.equals(result);
    }

    public boolean shouldNotSave() {
        return CLOSE_WITHOUT_SAVE.equals(result);
    }

    public boolean shouldCancel() {
        return result == null || CANCEL.equals(result);
    }
}


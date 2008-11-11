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
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 */
public class OverwriteDialog {

    private static final String SELECT_ANOTHER = "Select Another";
    private static final String OVERWRITE = "Overwrite";
    private static final String CANCEL = "Cancel";
    private static final String[] OPTIONS = new String[] {
            SELECT_ANOTHER, OVERWRITE, CANCEL
    };

    protected File file;
    protected Component parent;
    protected String result = CANCEL;

    public OverwriteDialog(File file, Component parent) {
        this.file = file;
        this.parent = parent;
    }

    public void show() {
        JOptionPane pane = new JOptionPane("Do you want to overwrite an existing file: "
                + file, JOptionPane.QUESTION_MESSAGE);
        pane.setOptions(OPTIONS);

        JDialog dialog = pane.createDialog(parent, "File exists");
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        result = (selectedValue != null) ? selectedValue.toString() : CANCEL;
    }

    public boolean shouldSelectAnother() {
        return SELECT_ANOTHER.equals(result);
    }

    public boolean shouldOverwrite() {
        return OVERWRITE.equals(result);
    }

    public boolean shouldCancel() {
        return result == null || CANCEL.equals(result);
    }
}

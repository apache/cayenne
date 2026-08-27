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

package org.apache.cayenne.modeler.ui.project.overwrite;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class OverwriteDialog {

    private static final String SELECT_ANOTHER = "Select Another";
    private static final String OVERWRITE = "Overwrite";
    private static final String CANCEL = "Cancel";

    private final File file;
    private final Component parent;
    private String result;

    public OverwriteDialog(File file, Component parent) {
        this.file = file;
        this.parent = parent;
        this.result = CANCEL;
    }

    public void show() {
        String message = "Do you want to overwrite the existing file: " + file;
        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE);
        pane.setOptions(new String[]{
                SELECT_ANOTHER, OVERWRITE, CANCEL
        });

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
}

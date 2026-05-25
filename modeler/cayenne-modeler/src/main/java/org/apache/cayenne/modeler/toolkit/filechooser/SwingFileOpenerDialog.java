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

package org.apache.cayenne.modeler.toolkit.filechooser;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.io.File;

/**
 * Modal open-file dialog backed by {@link JFileChooser#showOpenDialog}.
 */
public final class SwingFileOpenerDialog extends JFileChooser {

    private final Component parent;

    public SwingFileOpenerDialog(
            Component parent,
            String title,
            File startDir,
            FileFilter filter) {

        super(startDir);

        this.parent = parent;
        setFileSelectionMode(FILES_ONLY);
        if (title != null) {
            setDialogTitle(title);
        }
        if (filter != null) {
            addChoosableFileFilter(filter);
            setFileFilter(filter);
        }
    }

    /**
     * Shows the dialog modally and returns the selected file, or {@code null} if the user cancelled.
     */
    public File open() {
        return showOpenDialog(parent) == APPROVE_OPTION ? getSelectedFile() : null;
    }
}

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

import org.apache.cayenne.modeler.pref.adapters.FileChooserPrefs;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Modal open-file dialog backed by {@link JFileChooser#showOpenDialog}.
 */
public final class SwingFileOpenerDialog extends JFileChooser {

    private static SwingFileOpenerDialog instance;

    /**
     * Returns the lazily-initialized cached singleton. Constructing a {@link JFileChooser} is
     * expensive — on Windows it triggers a full L&amp;F UI delegate install and a Shell32 scan
     * that can take hundreds of milliseconds, making first-time dialog open feel sluggish.
     * Since Swing is single-threaded, one instance is safely reused across all callers; transient
     * state (listeners, filters, current directory, selection) is reset on each {@link #open}.
     */
    public static SwingFileOpenerDialog getInstance() {
        if (instance == null) {
            instance = new SwingFileOpenerDialog();
        }
        return instance;
    }

    private SwingFileOpenerDialog() {
        setFileSelectionMode(FILES_ONLY);
    }

    /**
     * Shows the dialog modally and returns the selected file, or {@code null} if the user cancelled.
     */
    public File open(Component parent, String title, File startDir, FileFilter filter, FileChooserPrefs prefs) {
        reset();
        if (startDir != null) {
            setCurrentDirectory(startDir);
        }
        setDialogTitle(title);
        if (filter != null) {
            addChoosableFileFilter(filter);
            setFileFilter(filter);
        }
        if (prefs != null) {
            prefs.bind(this);
        }
        return showOpenDialog(parent) == APPROVE_OPTION ? getSelectedFile() : null;
    }

    private void reset() {
        for (ActionListener l : getActionListeners()) {
            removeActionListener(l);
        }
        resetChoosableFileFilters();
        setSelectedFile(null);
    }
}

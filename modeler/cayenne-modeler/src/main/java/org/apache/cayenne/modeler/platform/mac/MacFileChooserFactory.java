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

package org.apache.cayenne.modeler.platform.mac;

import org.apache.cayenne.modeler.toolkit.filechooser.FileChooserFactory;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.FilenameFilter;

/**
 * {@link FileChooserFactory} implementation backed by the native OS {@link FileDialog}.
 * On macOS this delegates to NSOpenPanel / NSSavePanel, providing Quick Look preview,
 * Spotlight search, and the standard Finder sidebar.
 *
 * <p>Directory picking requires {@code apple.awt.fileDialogForDirectories=true} to be set
 * as a global JVM property before Swing starts (set by {@code MacUIInitializer}).
 */
public class MacFileChooserFactory extends FileChooserFactory {

    private static final String DIRS_DIALOG_PROPERTY = "apple.awt.fileDialogForDirectories";

    @Override
    public File openFile(Component parent, String title, File initialDir, FileFilter filter) {
        Frame frame = toFrame(parent);
        String titleStr = title != null ? title : "";
        FilenameFilter fnFilter = filter != null
                ? (dir, name) -> filter.accept(new File(dir, name))
                : null;

        File startDir = initialDir;
        while (true) {
            FileDialog fd = new FileDialog(frame, titleStr, FileDialog.LOAD);
            if (startDir != null) {
                fd.setDirectory(startDir.getAbsolutePath());
            }
            if (fnFilter != null) {
                fd.setFilenameFilter(fnFilter);
            }
            fd.setVisible(true);

            String file = fd.getFile();
            if (file == null) {
                return null; // canceled
            }
            File selected = new File(fd.getDirectory(), file);
            if (selected.isFile()) {
                return selected;
            }
            // User selected a directory (possible when apple.awt.fileDialogForDirectories=true);
            // re-show starting inside it so they can navigate to a file.
            startDir = selected;
        }
    }

    @Override
    public File saveFile(Component parent, String title, File initialDir, String defaultName) {
        FileDialog fd = new FileDialog(toFrame(parent), title != null ? title : "", FileDialog.SAVE);
        if (initialDir != null) {
            fd.setDirectory(initialDir.getAbsolutePath());
        }
        if (defaultName != null) {
            fd.setFile(defaultName);
        }
        fd.setVisible(true);
        String file = fd.getFile();
        return file != null ? new File(fd.getDirectory(), file) : null;
    }

    @Override
    public File openDirectory(Component parent, String title, File initialDir) {
        // Toggle the property only for the duration of this modal dialog.
        // FileDialog is EDT-blocking, so no other dialog can run concurrently.
        System.setProperty(DIRS_DIALOG_PROPERTY, "true");
        try {
            FileDialog fd = new FileDialog(toFrame(parent), title != null ? title : "", FileDialog.LOAD);
            if (initialDir != null) {
                fd.setDirectory(initialDir.getAbsolutePath());
            }
            fd.setVisible(true);
            String file = fd.getFile();
            // getFile() returns the selected directory name; getDirectory() is its parent.
            return file != null ? new File(fd.getDirectory(), file) : null;
        } finally {
            System.clearProperty(DIRS_DIALOG_PROPERTY);
        }
    }

    private static Frame toFrame(Component c) {
        Window w = SwingUtilities.getWindowAncestor(c);
        while (w != null) {
            if (w instanceof Frame f) {
                return f;
            }
            w = w.getOwner();
        }
        return null;
    }
}

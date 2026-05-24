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

import org.apache.cayenne.modeler.pref.adapters.FileChooserPrefs;
import org.apache.cayenne.modeler.toolkit.filechooser.FileChooserFactory;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.FilenameFilter;
import java.util.function.Consumer;

/**
 * {@link FileChooserFactory} implementation backed by the native OS {@link FileDialog}.
 * On macOS this delegates to NSOpenPanel / NSSavePanel, providing Quick Look preview,
 * Spotlight search, and the standard Finder sidebar.
 */
public class MacFileChooserFactory implements FileChooserFactory {

    private static final String DIRS_DIALOG_PROPERTY = "apple.awt.fileDialogForDirectories";

    @Override
    public File openFile(Component parent, String title, File initialDir, FileFilter filter) {
        return showOpen(toFrame(parent), title, toFilenameFilter(filter),
                fd -> { if (initialDir != null) fd.setDirectory(initialDir.getAbsolutePath()); });
    }

    @Override
    public File openFile(Component parent, String title, FileChooserPrefs prefs, FileFilter filter) {
        return showOpen(toFrame(parent), title, toFilenameFilter(filter), prefs::bind);
    }

    @Override
    public File saveFile(Component parent, String title, FileChooserPrefs prefs, String defaultName) {
        return showSave(toFrame(parent), title, prefs::bind, defaultName);
    }

    @Override
    public File openDir(Component parent, String title, File initialDir) {
        return showOpenDir(toFrame(parent), title,
                fd -> { if (initialDir != null) fd.setDirectory(initialDir.getAbsolutePath()); });
    }

    @Override
    public File openDir(Component parent, String title, FileChooserPrefs prefs) {
        return showOpenDir(toFrame(parent), title, prefs::bind);
    }

    @Override
    public File saveDir(Component parent, String title, File initialDir) {
        return showOpenDir(toFrame(parent), title,
                fd -> { if (initialDir != null) fd.setDirectory(initialDir.getAbsolutePath()); });
    }

    private File showOpen(Frame frame, String title, FilenameFilter fnFilter, Consumer<FileDialog> init) {
        FileDialog fd = new FileDialog(frame, title != null ? title : "", FileDialog.LOAD);
        init.accept(fd);
        if (fnFilter != null) {
            fd.setFilenameFilter(fnFilter);
        }
        fd.setVisible(true);
        String file = fd.getFile();
        return file != null ? new File(fd.getDirectory(), file) : null;
    }

    private File showSave(Frame frame, String title, Consumer<FileDialog> init, String defaultName) {
        FileDialog fd = new FileDialog(frame, title != null ? title : "", FileDialog.SAVE);
        init.accept(fd);
        if (defaultName != null) {
            fd.setFile(defaultName);
        }
        fd.setVisible(true);
        String file = fd.getFile();
        return file != null ? new File(fd.getDirectory(), file) : null;
    }

    private File showOpenDir(Frame frame, String title, Consumer<FileDialog> init) {
        // Toggle the property only for the duration of this modal dialog.
        // FileDialog is EDT-blocking, so no other dialog can run concurrently.
        System.setProperty(DIRS_DIALOG_PROPERTY, "true");
        try {
            FileDialog fd = new FileDialog(frame, title != null ? title : "", FileDialog.LOAD);
            init.accept(fd);
            fd.setVisible(true);
            String file = fd.getFile();
            // getFile() returns the selected directory name; getDirectory() is its parent.
            return file != null ? new File(fd.getDirectory(), file) : null;
        } finally {
            System.clearProperty(DIRS_DIALOG_PROPERTY);
        }
    }

    private static FilenameFilter toFilenameFilter(FileFilter filter) {
        return filter != null ? (dir, name) -> filter.accept(new File(dir, name)) : null;
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

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
import org.apache.cayenne.modeler.toolkit.filechooser.CMFileChooser;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.function.Consumer;

/**
 * {@link CMFileChooser} implementation backed by the native OS {@link FileDialog}.
 * On macOS this delegates to NSOpenPanel / NSSavePanel, providing Quick Look preview,
 * Spotlight search, and the standard Finder sidebar.
 */
public class MacFileChooser implements CMFileChooser {

    private final Frame parent;
    private final String title;

    public MacFileChooser(Frame parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    @Override
    public File openFile(File initialDir, FileFilter filter) {
        return showOpen(toFilenameFilter(filter),
                fd -> { if (initialDir != null) fd.setDirectory(initialDir.getAbsolutePath()); });
    }

    @Override
    public File openFile(FileChooserPrefs prefs, FileFilter filter) {
        return showOpen(toFilenameFilter(filter), prefs::bind);
    }

    @Override
    public File openDir(File initialDir) {
        return showOpenDir(c -> { if (initialDir != null) c.setCurrentDirectory(initialDir); });
    }

    @Override
    public File openDir(FileChooserPrefs prefs) {
        return showOpenDir(prefs::bind);
    }

    @Override
    public File saveFile(FileChooserPrefs prefs, String defaultName) {
        return showSave(prefs::bind, defaultName);
    }

    @Override
    public File saveDir(File initialDir) {
        return showOpenDir(c -> { if (initialDir != null) c.setCurrentDirectory(initialDir); });
    }

    private File showOpen(FilenameFilter fnFilter, Consumer<FileDialog> init) {
        FileDialog fd = new FileDialog(parent, title != null ? title : "", FileDialog.LOAD);
        init.accept(fd);
        if (fnFilter != null) {
            fd.setFilenameFilter(fnFilter);
        }
        fd.setVisible(true);
        String file = fd.getFile();
        return file != null ? new File(fd.getDirectory(), file) : null;
    }

    private File showSave(Consumer<FileDialog> init, String defaultName) {
        FileDialog fd = new FileDialog(parent, title != null ? title : "", FileDialog.SAVE);
        init.accept(fd);
        if (defaultName != null) {
            fd.setFile(defaultName);
        }
        fd.setVisible(true);
        String file = fd.getFile();
        return file != null ? new File(fd.getDirectory(), file) : null;
    }

    private File showOpenDir(Consumer<JFileChooser> init) {

        // JFileChooser instead of FileDialog: apple.awt.fileDialogForDirectories makes
        // FileDialog pick directories, but the button is labeled "Load" with no way to
        // override it. JFileChooser shows "Open" and lets us set a dialog title.

        // FILES_AND_DIRECTORIES + directory filter instead of DIRECTORIES_ONLY: on macOS
        // Aqua L&F the approval button stays disabled in DIRECTORIES_ONLY mode.
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(DIRS_ONLY_FILTER);

        // AquaFileChooserUI disables the approve button when the user navigates into a
        // directory with nothing selected. Directly enabling buttons in the component tree
        // (via invokeLater, after Aqua's own listener runs) keeps it always active so the
        // user can confirm the current directory without selecting a child item.
        // No state mutations — setSelectedFile(dir) makes Aqua navigate into that dir,
        // firing DIRECTORY_CHANGED again and causing an infinite loop.
        chooser.addPropertyChangeListener(e -> {
            String prop = e.getPropertyName();
            if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)
                    || JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
                SwingUtilities.invokeLater(() -> enableButtons(chooser));
            }
        });

        chooser.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && chooser.isShowing()) {
                enableButtons(chooser);
            }
        });
        
        init.accept(chooser);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File selected = chooser.getSelectedFile();
        return selected != null ? selected : chooser.getCurrentDirectory();
    }

    private static void enableButtons(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton btn) {
                btn.setEnabled(true);
            } else if (c instanceof Container cont) {
                enableButtons(cont);
            }
        }
    }

    private static final FileFilter DIRS_ONLY_FILTER = new FileFilter() {
        @Override public boolean accept(File f) { return f.isDirectory(); }
        @Override public String getDescription() { return "Directories"; }
    };

    private static FilenameFilter toFilenameFilter(FileFilter filter) {
        return filter != null ? (dir, name) -> filter.accept(new File(dir, name)) : null;
    }
}

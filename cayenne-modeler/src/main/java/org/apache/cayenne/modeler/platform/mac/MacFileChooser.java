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
import org.apache.cayenne.modeler.toolkit.filechooser.SwingDirChooserDialog;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
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
        return showOpen(toFilenameFilter(filter), initialDir, fd -> {});
    }

    @Override
    public File openFile(FileChooserPrefs prefs, FileFilter filter) {
        return showOpen(toFilenameFilter(filter), prefs.getDir(), prefs::bind);
    }

    @Override
    public File openDir(File initialDir) {
        // must use custom JFileChooser, as macOS native dialog doesn't allow to block file name selection
        return SwingDirChooserDialog.getInstance().open(parent, title, initialDir, null);
    }

    @Override
    public File openDir(FileChooserPrefs prefs) {
        // must use custom JFileChooser, as macOS native dialog doesn't allow to block file name selection
        return SwingDirChooserDialog.getInstance().open(parent, title, prefs.getDir(), prefs);
    }

    @Override
    public File saveFile(FileChooserPrefs prefs, String defaultName) {
        return showSave(prefs.getDir(), prefs::bind, defaultName);
    }

    @Override
    public File saveDir(File initialDir) {
        // must use custom JFileChooser, as macOS native dialog doesn't allow to block file name selection
        return SwingDirChooserDialog.getInstance().open(parent, title, initialDir, null);
    }

    private File showOpen(FilenameFilter fnFilter, File startDir, Consumer<FileDialog> init) {
        FileDialog fd = new FileDialog(parent, title != null ? title : "", FileDialog.LOAD);
        if (startDir != null) {
            fd.setDirectory(startDir.getAbsolutePath());
        }
        init.accept(fd);
        if (fnFilter != null) {
            fd.setFilenameFilter(fnFilter);
        }
        fd.setVisible(true);
        String file = fd.getFile();
        return file != null ? new File(fd.getDirectory(), file) : null;
    }

    private File showSave(File startDir, Consumer<FileDialog> init, String defaultName) {
        FileDialog fd = new FileDialog(parent, title != null ? title : "", FileDialog.SAVE);
        if (startDir != null) {
            fd.setDirectory(startDir.getAbsolutePath());
        }
        init.accept(fd);
        if (defaultName != null) {
            fd.setFile(defaultName);
        }
        fd.setVisible(true);
        String file = fd.getFile();
        return file != null ? new File(fd.getDirectory(), file) : null;
    }

    private static FilenameFilter toFilenameFilter(FileFilter filter) {
        return filter != null ? (dir, name) -> filter.accept(new File(dir, name)) : null;
    }
}

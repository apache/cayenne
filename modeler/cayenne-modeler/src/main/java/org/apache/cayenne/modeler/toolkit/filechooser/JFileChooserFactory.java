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
import java.io.File;
import java.util.function.Consumer;

/**
 * {@link FileChooserFactory} implementation backed by Swing's {@link JFileChooser}.
 */
public class JFileChooserFactory implements FileChooserFactory {

    @Override
    public File openFile(Component parent, String title, File initialDir, FileFilter filter) {
        return showOpen(
                parent,
                title,
                JFileChooser.FILES_ONLY,
                c -> {
                    if (initialDir != null) c.setCurrentDirectory(initialDir);
                },
                filter);
    }

    @Override
    public File openFile(Component parent, String title, FileChooserPrefs prefs, FileFilter filter) {
        return showOpen(parent, title, JFileChooser.FILES_ONLY, prefs::bind, filter);
    }

    @Override
    public File saveFile(Component parent, String title, FileChooserPrefs prefs, String defaultName) {
        return showSave(parent, title, prefs::bind, defaultName);
    }

    @Override
    public File openDir(Component parent, String title, File initialDir) {
        return showOpen(parent, title, JFileChooser.DIRECTORIES_ONLY,
                c -> {
                    if (initialDir != null) c.setCurrentDirectory(initialDir);
                }, null);
    }

    @Override
    public File openDir(Component parent, String title, FileChooserPrefs prefs) {
        return showOpen(parent, title, JFileChooser.DIRECTORIES_ONLY, prefs::bind, null);
    }

    @Override
    public File saveDir(Component parent, String title, File initialDir) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (initialDir != null) {
            chooser.setCurrentDirectory(initialDir);
        }
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        return chooser.showDialog(parent, "Select") == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    private File showOpen(Component parent, String title, int mode, Consumer<JFileChooser> init, FileFilter filter) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(mode);
        init.accept(chooser);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
            chooser.setFileFilter(filter);
        }
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    private File showSave(Component parent, String title, Consumer<JFileChooser> init, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        init.accept(chooser);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (defaultName != null) {
            chooser.setSelectedFile(new File(defaultName));
        }
        return chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }
}

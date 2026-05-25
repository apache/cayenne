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

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

public class SwingFileChooser implements CMFileChooser {

    private final Component parent;
    private final String title;

    public SwingFileChooser(Component parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    @Override
    public File openFile(File initialDir, FileFilter filter) {
        return new SwingFileOpenerDialog(parent, title, initialDir, filter).open();
    }

    @Override
    public File openFile(FileChooserPrefs prefs, FileFilter filter) {
        SwingFileOpenerDialog dialog = new SwingFileOpenerDialog(parent, title, prefs.getDir(), filter);
        prefs.bind(dialog);
        return dialog.open();
    }

    @Override
    public File openDir(File initialDir) {
        return new SwingDirChooserDialog(parent, title, initialDir).open();
    }

    @Override
    public File openDir(FileChooserPrefs prefs) {
        SwingDirChooserDialog dialog = new SwingDirChooserDialog(parent, title, prefs.getDir());
        prefs.bind(dialog);
        return dialog.open();
    }

    @Override
    public File saveFile(FileChooserPrefs prefs, String defaultName) {
        SwingFileSaverDialog dialog = new SwingFileSaverDialog(parent, title, prefs.getDir(), defaultName);
        prefs.bind(dialog);
        return dialog.open();
    }

    @Override
    public File saveDir(File initialDir) {
        return new SwingDirChooserDialog(parent, title, initialDir).open();
    }
}

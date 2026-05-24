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

package org.apache.cayenne.modeler.pref.adapters;

import org.apache.cayenne.modeler.pref.PrefsAdapter;

import javax.swing.JFileChooser;
import java.awt.FileDialog;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.prefs.Preferences;

public final class FileChooserPrefs extends PrefsAdapter {

    private static final String PATH_PROPERTY = "path";

    public FileChooserPrefs(Preferences prefs) {
        super(prefs);
    }

    public void bind(JFileChooser chooser) {
        File startDir = loadDir();
        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }
        chooser.addActionListener(e -> {
            if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                saveDir(chooser.getSelectedFile());
            }
        });
    }

    public void bind(FileDialog dialog) {
        File startDir = loadDir();
        if (startDir != null) {
            dialog.setDirectory(startDir.getAbsolutePath());
        }
        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                String file = dialog.getFile();
                String dir = dialog.getDirectory();
                if (file != null && dir != null) {
                    prefs.put(PATH_PROPERTY, dir);
                }
            }
        });
    }

    public File loadDir() {
        return resolveExistingDirectory(prefs.get(PATH_PROPERTY, null));
    }

    public void saveDir(File f) {
        if (f != null) {
            String dir = f.isFile() ? f.getParentFile().getAbsolutePath() : f.getAbsolutePath();
            prefs.put(PATH_PROPERTY, dir);
        }
    }

    private static File resolveExistingDirectory(String path) {
        if (path == null) {
            return null;
        }

        File f = new File(path);
        if (f.isDirectory()) {
            return f;
        }

        if (f.isFile()) {
            return f.getParentFile();
        }

        return null;
    }
}

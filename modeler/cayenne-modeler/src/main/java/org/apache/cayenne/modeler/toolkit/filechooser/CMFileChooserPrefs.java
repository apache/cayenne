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

import org.apache.cayenne.modeler.pref.PreferenceAdapter;

import javax.swing.JFileChooser;
import java.io.File;
import java.util.prefs.Preferences;

public final class CMFileChooserPrefs extends PreferenceAdapter {

    private static final String PATH_PROPERTY = "path";

    public CMFileChooserPrefs(Preferences prefs) {
        super(prefs);
    }

    public void bind(JFileChooser chooser) {
        File startDir = resolveExistingDirectory(prefs.get(PATH_PROPERTY, null));
        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }

        chooser.addActionListener(e -> {
            if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                File selected = chooser.getSelectedFile();
                if (selected != null) {
                    prefs.put(PATH_PROPERTY,
                            selected.isFile()
                                    ? selected.getParentFile().getAbsolutePath()
                                    : selected.getAbsolutePath());
                }
            }
        });
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

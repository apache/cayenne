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
import java.awt.Component;
import java.io.File;

/**
 * Platform-specific factory for file and directory chooser dialogs. Implementations may use either a native OS dialog
 * or Swing's JFileChooser.
 */
public interface FileChooserFactory {

    File openFile(Component parent, String title, File initialDir, FileFilter filter);

    File openFile(Component parent, String title, FileChooserPrefs prefs, FileFilter filter);

    File openDir(Component parent, String title, File initialDir);

    File openDir(Component parent, String title, FileChooserPrefs prefs);

    File saveFile(Component parent, String title, FileChooserPrefs prefs, String defaultName);

    File saveDir(Component parent, String title, File initialDir);
}

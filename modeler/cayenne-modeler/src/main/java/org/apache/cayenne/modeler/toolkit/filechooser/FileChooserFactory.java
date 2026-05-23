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

import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.io.File;

/**
 * Platform-specific factory for file and directory chooser dialogs.
 * Implementations may use either a native OS dialog or Swing's JFileChooser.
 */
public abstract class FileChooserFactory {

    /**
     * Shows a file-open dialog and returns the selected file, or {@code null} if canceled.
     */
    public abstract File openFile(Component parent, String title, File initialDir, FileFilter filter);

    /**
     * Shows a file-save dialog and returns the selected file, or {@code null} if canceled.
     */
    public abstract File saveFile(Component parent, String title, File initialDir, String defaultName);

    /**
     * Shows a directory-selection dialog and returns the selected directory, or {@code null} if canceled.
     */
    public abstract File openDirectory(Component parent, String title, File initialDir);
}

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

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.io.File;

/**
 * {@link FileChooserFactory} implementation backed by Swing's {@link JFileChooser}.
 */
public class JFileChooserFactory extends FileChooserFactory {

    @Override
    public File openFile(Component parent, String title, File initialDir, FileFilter filter) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (initialDir != null) {
            chooser.setCurrentDirectory(initialDir);
        }
        if (filter != null) {
            chooser.addChoosableFileFilter(filter);
            chooser.setFileFilter(filter);
        }
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    @Override
    public File saveFile(Component parent, String title, File initialDir, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (initialDir != null) {
            chooser.setCurrentDirectory(initialDir);
        }
        if (defaultName != null) {
            chooser.setSelectedFile(new File(defaultName));
        }
        return chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    @Override
    public File openDirectory(Component parent, String title, File initialDir) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (initialDir != null) {
            chooser.setCurrentDirectory(initialDir);
        }
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }
}

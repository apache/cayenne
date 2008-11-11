/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.pref;

import java.io.File;

import javax.swing.JFileChooser;

/**
 * Represents a preferred directory or file.
 * 
 */
public class FSPath extends _FSPath {

    public void updateFromChooser(JFileChooser chooser) {
        File file = chooser.getSelectedFile();
        if (file != null) {
            setDirectory(file);
        }
    }

    public void updateChooser(JFileChooser chooser) {
        File startDir = getExistingDirectory(false);
        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }
    }

    public void setDirectory(File file) {

        if (file.isFile()) {
            setPath(file.getParentFile().getAbsolutePath());
        }
        else {
            setPath(file.getAbsolutePath());
        }
    }

    public File getExistingDirectory(boolean create) {
        if (getPath() == null) {
            return null;
        }

        File path = new File(getPath());
        if (path.isDirectory()) {
            return path;
        }

        if (path.isFile()) {
            return path.getParentFile();
        }

        if (create) {
            path.mkdirs();
            return path;
        }

        return null;
    }
}


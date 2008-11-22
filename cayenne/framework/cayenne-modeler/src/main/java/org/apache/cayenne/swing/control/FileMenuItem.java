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

package org.apache.cayenne.swing.control;

import java.io.File;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

/**
 * A menu item that points to a file.
 * 
 */
public class FileMenuItem extends JMenuItem {

    /**
     * Creates a new instance with the specified fileName.
     */
    public FileMenuItem(String fileName) {
        setText(fileName);
    }

    protected void configurePropertiesFromAction(Action a) {
        // excludes most generic action keys that are not applicable here...
        setIcon(a != null ? (Icon) a.getValue(Action.SMALL_ICON) : null);
        setEnabled(a != null ? a.isEnabled() : true);
    }

    /**
     * Returns a file if this menu item points to a readable file or directory, or null
     * otherwise.
     */
    public File getFile() {
        if (getText() == null) {
            return null;
        }

        File f = new File(getText());
        return f.canRead() ? f : null;
    }

}

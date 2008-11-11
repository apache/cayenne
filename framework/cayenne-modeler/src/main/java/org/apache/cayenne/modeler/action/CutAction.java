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
package org.apache.cayenne.modeler.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ProjectPath;

/**
 * Action for cutting entities, queries etc.
 */
public class CutAction extends CayenneAction {
    public static String getActionName() {
        return "Cut";
    }

    /**
     * Constructor for CutAction
     */
    public CutAction(Application application) {
        this(getActionName(), application);
    }
    
    /**
     * Constructor for descendants
     */
    protected CutAction(String name, Application application) {
        super(name, application);
    }

    @Override
    public String getIconName() {
        return "icon-cut.gif";
    }
    
    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * Performs cutting of items
     */
    @Override
    public void performAction(ActionEvent e) {
        application.getAction(CopyAction.getActionName()).performAction(e);
        ((RemoveAction) application.getAction(RemoveAction.getActionName())).performAction(e, false);
    }
    
    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
    @Override
    public boolean enableForPath(ProjectPath path) {
        return application.getAction(CopyAction.getActionName()).enableForPath(path);
    }
}

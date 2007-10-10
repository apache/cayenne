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

import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.FindDialog;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.map.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FindAction extends CayenneAction {
    private java.util.List paths;

    public static String getActionName() {
        return "Find";
    }

    public FindAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * All entities that contain a pattern substring (case-indifferent) in the name are produced.
     * @param e
     */
    public void performAction(ActionEvent e) {
        JTextField source = (JTextField) e.getSource();

        paths = new ArrayList();
        if (!source.getText().trim().equals("")) {
            Pattern pattern = Pattern.compile(source.getText().trim(), Pattern.CASE_INSENSITIVE);

            Iterator it = getProjectController().getProject().treeNodes();
            while(it.hasNext()) {
                ProjectPath path = (ProjectPath) it.next();

                Object o = path.getObject();
                if ((o instanceof ObjEntity || o instanceof DbEntity) && matchFound(((Entity) o).getName(), pattern))
                    paths.add(path.getPath());
                else if (o instanceof Attribute && matchFound(((Attribute) o).getName(), pattern))
                    paths.add(path.getPath());
                else if (o instanceof Relationship && matchFound(((Relationship) o).getName(), pattern))
                    paths.add(path.getPath());
            }
        }

        source.setText("");

        new FindDialog(getApplication().getFrameController(), paths).startupAction();
    }

    private boolean matchFound(String entityName, Pattern pattern) {
        Matcher m = pattern.matcher(entityName);

        return m.find();
    }
}

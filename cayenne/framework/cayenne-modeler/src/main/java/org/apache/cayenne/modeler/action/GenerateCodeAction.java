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

import java.awt.event.ActionEvent;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.codegen.CodeGeneratorController;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ProjectPath;

public class GenerateCodeAction extends CayenneAction {

    public static String getActionName() {
        return "Generate Classes";
    }

    public GenerateCodeAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-gen_java.gif";
    }

    public void performAction(ActionEvent e) {
        DataMap dataMap = getProjectController().getCurrentDataMap();
        if (dataMap != null) {
            new CodeGeneratorController(getApplication().getFrameController(), dataMap)
                    .startup();
        }
    }

    /**
     * Returns <code>true</code> if path contains a DataMap object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataMap.class) != null;
    }
}

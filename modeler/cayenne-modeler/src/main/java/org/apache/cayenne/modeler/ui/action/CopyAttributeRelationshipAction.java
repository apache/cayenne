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
package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.properties.DbAttributePanel;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjAttributePanel;
import javax.swing.JComponent;

public class CopyAttributeRelationshipAction extends CopyAction implements MultipleObjectsAction {

    private final CopyAttributeAction copyAttributeAction;
    private final CopyRelationshipAction copyRelationshipAction;
    private JComponent currentSelectedPanel;

    public CopyAttributeRelationshipAction(Application application) {
        super(application);
        copyAttributeAction = new CopyAttributeAction(application);
        copyRelationshipAction = new CopyRelationshipAction(application);
    }

    public void setCurrentSelectedPanel(JComponent currentSelectedPanel) {
        this.currentSelectedPanel = currentSelectedPanel;
    }

    public String getActionName(boolean multiple) {
        if (currentSelectedPanel instanceof ObjAttributePanel || currentSelectedPanel instanceof DbAttributePanel) {
            return copyAttributeAction.getActionName(multiple);
        } else {
            return copyRelationshipAction.getActionName(multiple);
        }
    }

    public boolean enableForPath(ConfigurationNode object) {
        if (currentSelectedPanel instanceof ObjAttributePanel || currentSelectedPanel instanceof DbAttributePanel) {
            return copyAttributeAction.enableForPath(object);
        } else {
            return copyRelationshipAction.enableForPath(object);
        }
    }

    public Object copy(ProjectController controller) {
        if (currentSelectedPanel instanceof ObjAttributePanel || currentSelectedPanel instanceof DbAttributePanel) {
            return copyAttributeAction.copy(controller);
        } else {
            return copyRelationshipAction.copy(controller);
        }
    }

}

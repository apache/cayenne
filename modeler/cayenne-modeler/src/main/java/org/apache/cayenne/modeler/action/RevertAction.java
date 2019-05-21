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

package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;
import java.io.File;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.Project;

/**
 */
public class RevertAction extends CayenneAction {

    public static String getActionName() {
        return "Revert";
    }

    public RevertAction(Application application) {
        super(getActionName(), application);
    }

    public void performAction(ActionEvent e) {

        Project project = getCurrentProject();
        if (project == null) {
            return;
        }

        boolean isNew = project.getConfigurationResource() == null;

        CayenneModelerController controller = getApplication().getFrameController();

        // close ... don't use OpenProjectAction close method as it will ask for save, we
        // don't want that here
        controller.projectClosedAction();

        File fileDirectory = new File(project
                .getConfigurationResource()
                .getURL()
                .getPath());
        // reopen existing
        if (!isNew && fileDirectory.isFile()) {
            OpenProjectAction openAction = controller
                    .getApplication()
                    .getActionManager()
                    .getAction(OpenProjectAction.class);
            openAction.openProject(fileDirectory);
        }
        // create new
        else if (!(project instanceof Project)) {
            throw new CayenneRuntimeException("Only ApplicationProjects are supported.");
        }
        else {
            controller.getApplication().getActionManager().getAction(
                    NewProjectAction.class).performAction(e);
        }

        application.getUndoManager().discardAllEdits();
    }
}

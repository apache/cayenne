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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.ui.unsavedchanges.UnsavedChangesDialog;

import java.awt.event.ActionEvent;


public class CloseProjectAction extends AppAction {

    public CloseProjectAction(Application application) {
        super(application, "Close Project");
    }

    @Override
    public void performAction(ActionEvent e) {
        closeProject(app, true);
    }

    /**
     * Returns true if successfully closed project, false otherwise.
     */
    public static boolean closeProject(Application app, boolean checkUnsaved) {
        // check if there is a project...
        ProjectSession session = app.getFrame().getProjectSession();
        if (session == null || session.project() == null) {
            return true;
        }

        if (checkUnsaved && !checkSaveOnClose(true, app)) {
            return false;
        }

        app.getUndoManager().discardAllEdits();
        app.getFrame().onProjectClosed();

        return true;
    }

    /**
     * Returns false if cancel closing the window, true otherwise.
     */
    public static boolean checkSaveOnClose(Object source, Application app) {
        ProjectSession session = app.getFrame().getProjectSession();
        if (session != null && session.isDirty()) {
            UnsavedChangesDialog dialog = new UnsavedChangesDialog(app.getFrame());
            dialog.show();

            if (dialog.shouldCancel()) {
                // discard changes and DO NOT close
                return false;
            } else if (dialog.shouldSave()) {
                // save changes and close
                ActionEvent e = new ActionEvent(
                        source,
                        ActionEvent.ACTION_PERFORMED,
                        "SaveAll");
                app
                        .getActionManager()
                        .getAction(SaveAction.class)
                        .actionPerformed(e);

                // save was canceled... do not close
                return !session.isDirty();
            }
        }

        return true;
    }
}

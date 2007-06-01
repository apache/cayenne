/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.CayenneModelerController;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.UnsavedChangesDialog;
import org.objectstyle.cayenne.modeler.util.CayenneAction;
import org.objectstyle.cayenne.project.ProjectPath;

/**
 * @author Andrei Adamchik
 */
public class ProjectAction extends CayenneAction {

    public static String getActionName() {
        return "Close Project";
    }

    public ProjectAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * Constructor for ProjectAction.
     * 
     * @param name
     */
    public ProjectAction(String name, Application application) {
        super(name, application);
    }

    /**
     * Closes current project.
     */
    public void performAction(ActionEvent e) {
        closeProject();
    }

    /** Returns true if successfully closed project, false otherwise. */
    public boolean closeProject() {
        // check if there is a project...
        if (getProjectController() == null || getProjectController().getProject() == null) {
            return true;
        }

        if (!checkSaveOnClose()) {
            return false;
        }

        CayenneModelerController controller = Application
                .getInstance()
                .getFrameController();
        controller.projectClosedAction();

        return true;
    }

    /**
     * Returns false if cancel closing the window, true otherwise.
     */
    public boolean checkSaveOnClose() {
        ProjectController projectController = getProjectController();
        if (projectController != null && projectController.isDirty()) {
            UnsavedChangesDialog dialog = new UnsavedChangesDialog(Application.getFrame());
            dialog.show();

            if (dialog.shouldCancel()) {
                // discard changes and DO NOT close
                return false;
            }
            else if (dialog.shouldSave()) {
                // save changes and close
                ActionEvent e = new ActionEvent(
                        this,
                        ActionEvent.ACTION_PERFORMED,
                        "SaveAll");
                Application
                        .getFrame()
                        .getAction(SaveAction.getActionName())
                        .actionPerformed(e);
                if (projectController.isDirty()) {
                    // save was canceled... do not close
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Always returns true.
     */
    public boolean enableForPath(ProjectPath path) {
        return true;
    }
}
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

import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.project.Project;

/**
 */
public class NewProjectAction extends ProjectAction {

    public static String getActionName() {
        return "New Project";
    }

    public NewProjectAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-new.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask());
    }

    public void performAction(ActionEvent e) {

        CayenneModelerController controller = Application
                .getInstance()
                .getFrameController();
        // Save and close (if needed) currently open project.
        if (getCurrentProject() != null && !closeProject(true)) {
            return;
        }

        DataChannelDescriptor domain = new DataChannelDescriptor();

        domain.setName(DefaultUniqueNameGenerator.generate(NameCheckers.DataChannelDescriptor, domain));

        Project project = new Project(
                new ConfigurationTree<DataChannelDescriptor>(domain));

        controller.projectOpenedAction(project);

        // select default domain
        getProjectController().fireDomainDisplayEvent(
                new DomainDisplayEvent(this, domain));
    }
}

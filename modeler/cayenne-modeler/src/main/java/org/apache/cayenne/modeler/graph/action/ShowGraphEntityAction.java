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
package org.apache.cayenne.modeler.graph.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * Action that shows entity on the graph
 */
public class ShowGraphEntityAction extends CayenneAction {

    public static String getActionName() {
        return "ShowGraphEntity";
    }

    public ShowGraphEntityAction(Application application) {
        super(getActionName(), application, "Show on Graph");
        setEnabled(true);
    }

    @Override
    public String getIconName() {
        return "icon-save-as-image.png";
    }

    @Override
    public void performAction(ActionEvent e) {
        Entity entity = null;

        ProjectController mediator = getProjectController();
        if (mediator.getCurrentDbEntity() != null) {
            entity = mediator.getCurrentDbEntity();
        }
        else if (mediator.getCurrentObjEntity() != null) {
            entity = mediator.getCurrentObjEntity();
        }

        if (entity != null) {
            showEntity(entity);
        }
    }

    @Override
    public boolean enableForPath(ConfigurationNode object) {
        return object instanceof Entity;
    }

    void showEntity(Entity entity) {
        // we're always in same domain
        EditorView editor = ((CayenneModelerFrame) Application
                .getInstance()
                .getFrameController()
                .getView()).getView();

        editor.getProjectTreeView().getSelectionModel().setSelectionPath(
                editor
                        .getProjectTreeView()
                        .getSelectionPath()
                        .getParentPath()
                        .getParentPath());
        DomainDisplayEvent event = new EntityDisplayEvent(
                editor.getProjectTreeView(),
                entity,
                entity.getDataMap(),
                (DataChannelDescriptor) getProjectController().getProject().getRootNode());
        getProjectController().fireDomainDisplayEvent(event);
    }
}

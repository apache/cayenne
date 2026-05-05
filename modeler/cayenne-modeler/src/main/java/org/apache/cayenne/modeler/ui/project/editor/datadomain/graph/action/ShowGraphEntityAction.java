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
package org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.ui.action.ModelerAbstractAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.ProjectView;

/**
 * Action that shows entity on the graph
 */
public class ShowGraphEntityAction extends ModelerAbstractAction {

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
        Entity<?,?,?> entity = null;

        ProjectSession session = getProjectSession();
        if (session.getSelectedDbEntity() != null) {
            entity = session.getSelectedDbEntity();
        } else if (session.getSelectedObjEntity() != null) {
            entity = session.getSelectedObjEntity();
        }

        if (entity != null) {
            showEntity(entity);
        }
    }

    @Override
    public boolean enableForPath(ConfigurationNode object) {
        return object instanceof Entity;
    }

    void showEntity(Entity<?,?,?> entity) {
        // we're always in same domain
        ProjectView editor = app.getFrame().getProjectView();

        editor.getProjectTreeView().getSelectionModel().setSelectionPath(
                editor
                        .getProjectTreeView()
                        .getSelectionPath()
                        .getParentPath()
                        .getParentPath());

        DataChannelDescriptor domain = (DataChannelDescriptor) getProjectSession().project().getRootNode();
        if (entity instanceof ObjEntity) {
            getProjectSession().displayObjEntity(new ObjEntityDisplayEvent(this, domain, entity.getDataMap(), (ObjEntity) entity));
        } else if (entity instanceof DbEntity) {
            getProjectSession().displayDbEntity(new DbEntityDisplayEvent(this, domain, entity.getDataMap(), (DbEntity) entity));
        }
    }
}

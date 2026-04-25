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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.ui.project.tree.ProjectTreeModel;

import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

public abstract class BaseViewEntityAction extends ModelerAbstractAction {

    abstract protected Entity<?, ?, ?> getEntity();

    public BaseViewEntityAction(String name, Application application) {
        super(name, application);
    }

    /**
     * @see ModelerAbstractAction#performAction(ActionEvent)
     */
    @Override
    public void performAction(ActionEvent e) {
        viewEntity();
    }

    protected void viewEntity() {
        Entity<?, ?, ?> entity = getEntity();
        if (entity != null) {
            navigateToEntity(entity);
        }
    }

    public void navigateToEntity(Entity<?, ?, ?> entity) {
        TreePath path = buildTreePath(entity);
        editor().getProjectTreeView().getSelectionModel().setSelectionPath(path);

        EntityDisplayEvent event = new EntityDisplayEvent(
                editor().getProjectTreeView(),
                entity,
                entity.getDataMap(),
                (DataChannelDescriptor) getProjectController().getProject().getRootNode());
        if (entity instanceof DbEntity) {
            getProjectController().displayDbEntity(event);
        } else if (entity instanceof ObjEntity) {
            getProjectController().displayObjEntity(event);
        }
    }

    private TreePath buildTreePath(Entity<?, ?, ?> entity) {

        DataChannelDescriptor domain = (DataChannelDescriptor) getCurrentProject().getRootNode();

        Object[] path = new Object[]{domain, entity.getDataMap(), entity};

        Object[] mutableTreeNodes = new Object[path.length];
        mutableTreeNodes[0] = ((ProjectTreeModel) editor().getProjectTreeView().getModel())
                .getRootNode();

        Object[] helper;
        for (int i = 1; i < path.length; i++) {
            helper = new Object[i];
            for (int j = 0; j < i; ) {
                helper[j] = path[++j];
            }
            mutableTreeNodes[i] = ((ProjectTreeModel) editor()
                    .getProjectTreeView()
                    .getModel()).getNodeForObjectPath(helper);
        }
        return new TreePath(mutableTreeNodes);
    }
}

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
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.ProjectTreeModel;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;

public class DbEntityCounterpartAction extends CayenneAction {

    public static String getActionName() {
        return "View related ObjEntity";
    }

    public DbEntityCounterpartAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-move_up.png";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        viewCounterpartEntity();
    }

    protected void viewCounterpartEntity() {
        ProjectController mediator = getProjectController();

        DbEntity dbEntity = mediator.getCurrentDbEntity();

        if (dbEntity == null) {
            return;
        }
        
        Iterator<ObjEntity> it = dbEntity.getDataMap().getMappedEntities(dbEntity).iterator();
        if (!it.hasNext()) {
            return;
        }

        ObjEntity entity = it.next();
        
        TreePath path = buildTreePath(entity);
        editor().getProjectTreeView().getSelectionModel().setSelectionPath(path);
        
        EntityDisplayEvent event = new EntityDisplayEvent(
                editor().getProjectTreeView(),
                entity,
                entity.getDataMap(),
                (DataChannelDescriptor) getProjectController().getProject().getRootNode());
        getProjectController().fireObjEntityDisplayEvent(event);
    }
    
    public static EditorView editor() {
        return ((CayenneModelerFrame) Application
                .getInstance()
                .getFrameController()
                .getView()).getView();
    }
    
    /**
     * Builds a tree path for a given path. Urgent for later selection.
     * 
     * @param path
     * @return tree path
     */
    public static TreePath buildTreePath(Entity entity) {
        DataChannelDescriptor domain = (DataChannelDescriptor) Application
                .getInstance()
                .getProject()
                .getRootNode();
        
        Object[] path = new Object[] {domain, entity.getDataMap(), entity};

        Object[] mutableTreeNodes = new Object[path.length];
        mutableTreeNodes[0] = ((ProjectTreeModel) editor().getProjectTreeView().getModel())
                .getRootNode();

        Object[] helper;
        for (int i = 1; i < path.length; i++) {
            helper = new Object[i];
            for (int j = 0; j < i;) {
                helper[j] = path[++j];
            }
            mutableTreeNodes[i] = ((ProjectTreeModel) editor()
                    .getProjectTreeView()
                    .getModel()).getNodeForObjectPath(helper);
        }
        return new TreePath(mutableTreeNodes);
    }
}

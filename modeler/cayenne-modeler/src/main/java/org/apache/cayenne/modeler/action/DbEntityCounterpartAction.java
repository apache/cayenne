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

import javax.swing.tree.TreePath;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
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
        viewCounterpartEntity(entity);
    }

    public void viewCounterpartEntity(ObjEntity entity) {        
        TreePath path = buildTreePath(entity);
        editor().getProjectTreeView().getSelectionModel().setSelectionPath(path);
        
        EntityDisplayEvent event = new EntityDisplayEvent(
                editor().getProjectTreeView(),
                entity,
                entity.getDataMap(),
                (DataChannelDescriptor) getProjectController().getProject().getRootNode());
        getProjectController().fireObjEntityDisplayEvent(event);
    }
    
    
}

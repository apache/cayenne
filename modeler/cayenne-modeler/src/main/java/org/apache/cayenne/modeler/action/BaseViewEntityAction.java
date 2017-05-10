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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;

import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

/**
 * @since 4.0
 */
public abstract class BaseViewEntityAction extends CayenneAction{

    protected ObjEntity objEntity;

    abstract protected Entity getEntity();

    public BaseViewEntityAction(String name, Application application) {
        super(name, application);
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    @Override
    public void performAction(ActionEvent e) {
        viewEntity();
    }

    protected void viewEntity() {
        ProjectController mediator = getProjectController();

        objEntity = mediator.getCurrentObjEntity();

        if (objEntity == null) {
            return;
        }

        Entity entity = getEntity();

        if(entity != null) {
            TreePath path = DbEntityCounterpartAction.buildTreePath(entity);
            DbEntityCounterpartAction.editor().getProjectTreeView().getSelectionModel().setSelectionPath(path);

            EntityDisplayEvent event = new EntityDisplayEvent(
                    DbEntityCounterpartAction.editor().getProjectTreeView(),
                    entity,
                    entity.getDataMap(),
                    (DataChannelDescriptor) getProjectController().getProject().getRootNode());

            if (entity instanceof DbEntity) {
                mediator.fireDbEntityDisplayEvent(event);
            } else if (entity instanceof ObjEntity){
                mediator.fireObjEntityDisplayEvent(event);
            }
        }
    }
}

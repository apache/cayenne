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

package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.validation.ValidationFailure;

/**
 * DataDomain validation message.
 * 
 */
public class EntityErrorMsg extends ValidationDisplayHandler {

    protected DataMap map;
    protected Entity<?,?,?> entity;

    /**
     * Constructor for EntityErrorMsg.
     */
    public EntityErrorMsg(ValidationFailure result) {
        super(result);

        Object object = result.getSource();
        entity = (Entity<?,?,?>) object;
        map = entity.getDataMap();
        domain = (DataChannelDescriptor) Application
                .getInstance()
                .getProject()
                .getRootNode();
    }

    public void displayField(ProjectController mediator, JFrame frame) {
        EntityDisplayEvent event = new EntityDisplayEvent(frame, entity, map, domain);
        if (entity instanceof ObjEntity) {
            mediator.fireObjEntityDisplayEvent(event);
        }
        else if (entity instanceof DbEntity) {
            mediator.fireDbEntityDisplayEvent(event);
        }
    }
}

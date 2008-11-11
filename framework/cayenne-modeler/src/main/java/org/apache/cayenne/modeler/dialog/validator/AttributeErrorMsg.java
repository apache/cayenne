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


package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.project.validator.ValidationInfo;

/**
 * Attribute validation message.
 * 
 */
public class AttributeErrorMsg extends ValidationDisplayHandler {

    protected DataMap map;
    protected Entity entity;
    protected Attribute attribute;

    /**
     * Constructor for AttributeErrorMsg.
     * 
     * @param result
     */
    public AttributeErrorMsg(ValidationInfo result) {
        super(result);

        Object[] path = result.getPath().getPath();
        int len = path.length;

        if (len >= 1) {
            attribute = (Attribute) path[len - 1];
        }

        if (len >= 2) {
            entity = (Entity) path[len - 2];
        }

        if (len >= 3) {
            map = (DataMap) path[len - 3];
        }

        if (len >= 4) {
            domain = (DataDomain) path[len - 4];
        }

    }

    public void displayField(ProjectController mediator, JFrame frame) {
        AttributeDisplayEvent event = new AttributeDisplayEvent(
                frame,
                attribute,
                entity,
                map,
                domain);

        // must first display entity, and then switch to relationship display .. so fire
        // twice
        if (entity instanceof ObjEntity) {
            mediator.fireObjEntityDisplayEvent(event);
            mediator.fireObjAttributeDisplayEvent(event);
        }
        else if (entity instanceof DbEntity) {
            mediator.fireDbEntityDisplayEvent(event);
            mediator.fireDbAttributeDisplayEvent(event);
        }
    }
}

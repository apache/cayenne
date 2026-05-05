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

package org.apache.cayenne.modeler.ui.project.validator;

import javax.swing.JFrame;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjRelationshipDisplayEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.validation.ValidationFailure;

/**
 * Relationship validation message.
 *
 */
public class RelationshipErrorMsg extends ValidationDisplayHandler {

    protected DataMap map;
    protected Entity<?,?,?> entity;
    protected Relationship<?,?,?> rel;

    /**
     * Constructor for RelationshipErrorMsg.
     */
    public RelationshipErrorMsg(ValidationFailure result) {
        super(result);
        Object object = result.getSource();
        rel = (Relationship<?,?,?>) object;
        entity = rel.getSourceEntity();
        map = entity.getDataMap();
    }

    public void displayField(ProjectSession session, JFrame frame) {
        // must first display entity, and then switch to relationship display .. so fire twice
        if (entity instanceof ObjEntity) {
            session.displayObjEntity(new ObjEntityDisplayEvent(frame, domain, map, (ObjEntity) entity));
            session.displayObjRelationship(new ObjRelationshipDisplayEvent(frame, domain, map, (ObjEntity) entity, (ObjRelationship) rel));
        } else if (entity instanceof DbEntity) {
            session.displayDbEntity(new DbEntityDisplayEvent(frame, domain, map, (DbEntity) entity));
            session.displayDbRelationship(new DbRelationshipDisplayEvent(frame, domain, map, (DbEntity) entity, (DbRelationship) rel));
        }
    }
}

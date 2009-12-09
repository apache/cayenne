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
package org.apache.cayenne.modeler.undo;

import java.util.Collection;

import javax.swing.undo.CompoundEdit;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.EntityMergeListener;

public class DbEntitySyncUndoableEdit extends CompoundEdit {

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public boolean canUndo() {
        return !edits.isEmpty();
    }

    private DataDomain domain;
    private DataMap map;

    public DbEntitySyncUndoableEdit(DataDomain domain, DataMap map) {
        super();
        this.domain = domain;
        this.map = map;
    }

    public class EntitySyncUndoableListener implements EntityMergeListener {

        private ObjEntity entity;

        public EntitySyncUndoableListener(ObjEntity entity) {
            this.entity = entity;
        }

        public void objRelationshipAdded(ObjRelationship rel) {
            addEdit(new CreateRelationshipUndoableEdit(entity, new ObjRelationship[] {
                rel
            }));
        }

        public void objAttributeAdded(ObjAttribute attr) {
            addEdit(new CreateAttributeUndoableEdit(domain, map, entity, attr));
        }
    }

    public class MeaningfulFKsUndoableEdit extends CompoundEdit {

        @Override
        public boolean isInProgress() {
            return false;
        }

        @Override
        public boolean canUndo() {
            return !edits.isEmpty();
        }

        public MeaningfulFKsUndoableEdit(ObjEntity entity, Collection<DbAttribute> dbAttrs) {
            for (DbAttribute da : dbAttrs) {
                ObjAttribute oa = entity.getAttributeForDbAttribute(da);
                while (oa != null) {
                    addEdit(new RemoveAttributeUndoableEdit(
                            domain,
                            map,
                            entity,
                            new ObjAttribute[] {
                                oa
                            }));
                    oa = entity.getAttributeForDbAttribute(da);
                }
            }
        }
    }

    @Override
    public String getRedoPresentationName() {
        return "Redo Db Entity Sync";
    }

    @Override
    public String getUndoPresentationName() {
        return "Undo Db Entity Sync";
    }
}

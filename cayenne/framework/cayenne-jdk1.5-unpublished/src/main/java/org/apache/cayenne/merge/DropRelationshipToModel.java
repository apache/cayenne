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
package org.apache.cayenne.merge;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

public class DropRelationshipToModel extends AbstractToModelToken {

    private DbEntity entity;
    private DbRelationship rel;

    public DropRelationshipToModel(DbEntity entity, DbRelationship rel) {
        this.entity = entity;
        this.rel = rel;
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createAddRelationshipToDb(entity, rel);
    }

    public void execute(MergerContext mergerContext) {
        remove(rel, true);
    }

    private void remove(DbRelationship rel, boolean reverse) {
        if (rel == null) {
            return;
        }
        if (reverse) {
            remove(rel.getReverseRelationship(), false);
        }

        DbEntity dbEntity = (DbEntity) rel.getSourceEntity();
        for (ObjEntity objEntity : objEntitiesMappedToDbEntity(dbEntity)) {
            remove(objEntity.getRelationshipForDbRelationship(rel), true);
        }
        
        rel.getSourceEntity().removeRelationship(rel.getName());
    }

    private void remove(ObjRelationship rel, boolean reverse) {
        if (rel == null) {
            return;
        }
        if (reverse) {
            remove(rel.getReverseRelationship(), false);
        }
        rel.getSourceEntity().removeRelationship(rel.getName());
    }

    public String getTokenName() {
        return "Drop Relationship";
    }

    public String getTokenValue() {
        StringBuilder s = new StringBuilder();
        s.append(rel.getSourceEntity().getName());
        s.append("->");
        s.append(rel.getTargetEntityName());
        return s.toString();
    }

}

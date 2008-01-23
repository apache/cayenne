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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * A {@link MergerToken} to remove a {@link DbAttribute} from a {@link DbEntity}.
 * 
 * @author halset
 */
public class DropColumnToModel extends AbstractToModelToken {

    private DbEntity entity;
    private DbAttribute column;

    public DropColumnToModel(DbEntity entity, DbAttribute column) {
        this.entity = entity;
        this.column = column;
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createAddColumnToDb(entity, column);
    }

    public DbEntity getEntity() {
        return entity;
    }

    public DbAttribute getAttribute() {
        return column;
    }

    public void execute(MergerContext mergerContext) {

        // remove relationships mapped to column
        for (DbRelationship dbRelationship : entity.getRelationships()) {
            for (DbJoin join : dbRelationship.getJoins()) {
                if (join.getSource() == column || join.getTarget() == column) {
                    remove(dbRelationship, true);
                }
            }
        }

        // remove ObjAttribute mapped to same column
        for (ObjEntity objEntity : objEntitiesMappedToDbEntity(entity)) {
            ObjAttribute objAttribute = objEntity.getAttributeForDbAttribute(column);
            if (objAttribute != null) {
                objEntity.removeAttribute(objAttribute.getName());
            }

        }

        // remove DbAttribute
        entity.removeAttribute(column.getName());
    }

    public String getTokenName() {
        return "Drop Column";
    }

    public String getTokenValue() {
        return entity.getName() + "." + column.getName();
    }

}

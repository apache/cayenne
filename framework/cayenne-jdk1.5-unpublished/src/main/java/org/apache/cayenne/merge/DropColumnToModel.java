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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * A {@link MergerToken} to remove a {@link DbAttribute} from a {@link DbEntity}.
 * 
 */
public class DropColumnToModel extends AbstractToModelToken.EntityAndColumn {

    public DropColumnToModel(DbEntity entity, DbAttribute column) {
        super(entity, column);
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createAddColumnToDb(getEntity(), getColumn());
    }

    public void execute(MergerContext mergerContext) {

        // remove relationships mapped to column. duplicate List to prevent
        // ConcurrentModificationException
        List<DbRelationship> dbRelationships = new ArrayList<DbRelationship>(getEntity()
                .getRelationships());
        for (DbRelationship dbRelationship : dbRelationships) {
            for (DbJoin join : dbRelationship.getJoins()) {
                if (join.getSource() == getColumn() || join.getTarget() == getColumn()) {
                    remove(mergerContext, dbRelationship, true);
                }
            }
        }

        // remove ObjAttribute mapped to same column
        for (ObjEntity objEntity : objEntitiesMappedToDbEntity(getEntity())) {
            ObjAttribute objAttribute = objEntity.getAttributeForDbAttribute(getColumn());
            if (objAttribute != null) {
                objEntity.removeAttribute(objAttribute.getName());
                mergerContext.getModelMergeDelegate().objAttributeRemoved(objAttribute);
            }

        }

        // remove DbAttribute
        getEntity().removeAttribute(getColumn().getName());

        mergerContext.getModelMergeDelegate().dbAttributeRemoved(getColumn());
    }

    public String getTokenName() {
        return "Drop Column";
    }

}

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

package org.apache.cayenne.dbsync.merge.token.model;

import org.apache.cayenne.dbsync.merge.context.MergerContext;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MergerToken} to remove a {@link DbAttribute} from a {@link DbEntity}.
 * 
 */
public class DropColumnToModel extends AbstractToModelToken.EntityAndColumn {

    public DropColumnToModel(DbEntity entity, DbAttribute column) {
        super("Drop Column", 25, entity, column);
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createAddColumnToDb(getEntity(), getColumn());
    }

    @Override
    public void execute(MergerContext mergerContext) {

        // remove relationships mapped to column. duplicate List to prevent
        // ConcurrentModificationException
        List<DbRelationship> dbRelationships = new ArrayList<>(getEntity()
                .getRelationships());
        for (DbRelationship dbRelationship : dbRelationships) {
            for (DbJoin join : dbRelationship.getJoins()) {
                if (join.getSource() == getColumn() || join.getTarget() == getColumn()) {
                    remove(mergerContext.getDelegate(), dbRelationship, true);
                }
            }
        }

        // remove ObjAttribute mapped to same column
        for (ObjEntity objEntity : getEntity().mappedObjEntities()) {
            ObjAttribute objAttribute = objEntity.getAttributeForDbAttribute(getColumn());
            if (objAttribute != null) {
                objEntity.removeAttribute(objAttribute.getName());
                mergerContext.getDelegate().objAttributeRemoved(objAttribute);
            }

        }

        // remove DbAttribute
        getEntity().removeAttribute(getColumn().getName());

        mergerContext.getDelegate().dbAttributeRemoved(getColumn());
    }
}

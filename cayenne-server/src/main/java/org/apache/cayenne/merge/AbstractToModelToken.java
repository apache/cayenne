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
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Common abstract superclass for all {@link MergerToken}s going from the database to the
 * model.
 * 
 */
public abstract class AbstractToModelToken implements MergerToken {

    private final String tokenName;

    protected AbstractToModelToken(String tokenName) {
        this.tokenName = tokenName;
    }

    @Override
    public final String getTokenName() {
        return tokenName;
    }

    public final MergeDirection getDirection() {
        return MergeDirection.TO_MODEL;
    }

    protected static void remove(ModelMergeDelegate mergerContext, DbRelationship rel, boolean reverse) {
        if (rel == null) {
            return;
        }
        if (reverse) {
            remove(mergerContext, rel.getReverseRelationship(), false);
        }

        DbEntity dbEntity = rel.getSourceEntity();
        for (ObjEntity objEntity : dbEntity.mappedObjEntities()) {
            remove(mergerContext, objEntity.getRelationshipForDbRelationship(rel), true);
        }
        
        rel.getSourceEntity().removeRelationship(rel.getName());
        mergerContext.dbRelationshipRemoved(rel);
    }

    protected static void remove(ModelMergeDelegate mergerContext, ObjRelationship rel, boolean reverse) {
        if (rel == null) {
            return;
        }
        if (reverse) {
            remove(mergerContext, rel.getReverseRelationship(), false);
        }
        rel.getSourceEntity().removeRelationship(rel.getName());
        mergerContext.objRelationshipRemoved(rel);
    }

    @Override
    public String toString() {
        return getTokenName() + ' ' + getTokenValue() + ' ' + getDirection();
    }
    
    abstract static class Entity extends AbstractToModelToken {
        
        private final DbEntity entity;

        protected Entity(String tokenName, DbEntity entity) {
            super(tokenName);
            this.entity = entity;
        }

        public DbEntity getEntity() {
            return entity;
        }
        
        public String getTokenValue() {
            return getEntity().getName();
        }
        
    }
    
    abstract static class EntityAndColumn extends Entity {
        
        private final DbAttribute column;
        
        protected EntityAndColumn(String tokenName, DbEntity entity, DbAttribute column) {
            super(tokenName, entity);
            this.column = column;
        }

        public DbAttribute getColumn() {
            return column;
        }

        @Override
        public String getTokenValue() {
            return getEntity().getName() + "." + getColumn().getName();
        }
        
    }


}

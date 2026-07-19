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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.Util;

import java.util.List;

/**
 * The default strategy for converting DB-layer to Object-layer names.
 *
 * @since 5.0
 */
public abstract class BaseObjectNameGenerator implements ObjectNameGenerator {

    protected abstract String dbEntityBaseName(String dbEntityName);

    @Override
    public String objEntityName(DbEntity dbEntity) {
        return Util.underscoredToJava(dbEntityBaseName(dbEntity.getName()), true);
    }

    @Override
    public String objAttributeName(DbAttribute dbAttribute) {
        return Util.underscoredToJava(dbAttribute.getName(), false);
    }

    @Override
    public String objRelationshipName(DbRelationship... relationshipChain) {

        if (relationshipChain == null || relationshipChain.length < 1) {
            throw new IllegalArgumentException("At least one relationship is expected");
        }

        DbRelationship first = relationshipChain[0];
        DbRelationship last = relationshipChain[relationshipChain.length - 1];
        return Util.underscoredToJava(relationshipBase(first.getJoins(), last.getTargetEntityName(), isToMany(relationshipChain)), false);
    }

    @Override
    public String dbRelationshipName(List<DbJoin> joins, boolean toMany) {

        if (joins == null || joins.isEmpty()) {
            throw new IllegalArgumentException("At least one join is expected");
        }

        String targetEntityName = joins.getFirst().getRelationship().getTargetEntityName();
        return Util.underscoredToJava(relationshipBase(joins, targetEntityName, toMany), false);
    }

    protected boolean isToMany(DbRelationship... relationshipChain) {

        for (DbRelationship r : relationshipChain) {
            if (r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    protected String relationshipBase(List<DbJoin> joins, String targetEntityName, boolean toMany) {
        return toMany
                ? toManyBase(targetEntityName)
                : toOneBase(joins, targetEntityName);
    }

    protected String toManyBase(String targetEntityName) {
        String baseName = dbEntityBaseName(targetEntityName);
        return EnglishInflector.pluralOf(baseName.toLowerCase());
    }


    protected String toOneBase(List<DbJoin> joins, String targetEntityName) {

        if (joins.isEmpty()) {
            // In case, when uses EditRelationship button, relationship doesn't exist => it doesn't have joins
            // and just return targetName
            return dbEntityBaseName(targetEntityName);
        }

        DbJoin join1 = joins.getFirst();

        // TODO: multi-join relationships

        // return the name of the FK column sans ID
        String fkColName = join1.getSourceName();
        if (fkColName == null) {
            return dbEntityBaseName(targetEntityName);
        } else if (fkColName.toUpperCase().endsWith("_ID") && fkColName.length() > 3) {
            return fkColName.substring(0, fkColName.length() - 3);
        } else if (fkColName.toUpperCase().endsWith("ID") && fkColName.length() > 2) {
            return fkColName.substring(0, fkColName.length() - 2);
        } else {
            return dbEntityBaseName(targetEntityName);
        }
    }
}

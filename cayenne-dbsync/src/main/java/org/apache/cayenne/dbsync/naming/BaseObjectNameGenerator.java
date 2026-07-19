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
                ? toManyBase(joins, targetEntityName)
                : toOneBase(joins, targetEntityName);
    }

    protected String toManyBase(List<DbJoin> joins, String targetEntityName) {
        String plural = EnglishInflector.pluralOf(dbEntityBaseName(targetEntityName).toLowerCase());
        String qualifier = toManyRoleQualifier(joins);
        return qualifier != null ? qualifier + "_" + plural : plural;
    }

    private String toManyRoleQualifier(List<DbJoin> joins) {

        if (joins.isEmpty()) {
            return null;
        }

        DbJoin join1 = joins.getFirst();
        DbEntity sourceEntity = join1.getRelationship().getSourceEntity();

        // the FK column of a to-many relationship is on the target side of the join
        String fkColName = join1.getTargetName();
        if (sourceEntity == null || fkColName == null) {
            return null;
        }

        String fkBase = stripIdSuffix(fkColName);
        String role = dbEntityBaseName(fkBase != null ? fkBase : fkColName);
        String roleUpper = role.toUpperCase();

        // match the role against the source entity name, then against its shorter "_"-token suffixes,
        // as FK columns often drop a common table-name prefix ("home_team_id" referencing "nhl_team")
        String suffix = dbEntityBaseName(sourceEntity.getName()).toUpperCase();
        while (true) {
            if (roleUpper.equals(suffix)) {
                return null;
            }

            if (roleUpper.endsWith("_" + suffix)) {
                String qualifier = role.substring(0, role.length() - suffix.length() - 1);
                return qualifier.isEmpty() ? null : qualifier;
            }

            int underscore = suffix.indexOf('_');
            if (underscore < 0) {
                return null;
            }
            suffix = suffix.substring(underscore + 1);
        }
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
        }

        String fkBase = stripIdSuffix(fkColName);
        return fkBase != null ? fkBase : dbEntityBaseName(targetEntityName);
    }

    private static String stripIdSuffix(String fkColName) {
        if (fkColName.toUpperCase().endsWith("_ID") && fkColName.length() > 3) {
            return fkColName.substring(0, fkColName.length() - 3);
        } else if (fkColName.toUpperCase().endsWith("ID") && fkColName.length() > 2) {
            return fkColName.substring(0, fkColName.length() - 2);
        } else {
            return null;
        }
    }
}

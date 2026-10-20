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

package org.apache.cayenne.modeler.project;

import org.apache.cayenne.project.Project;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DbRelationshipOps {

    public static Collection<ObjRelationship> objRelationshipsUsingDbRelationship(
            Project project,
            DbRelationship relationship) {
        List<ObjRelationship> objRelationships = new ArrayList<>();
        if (project != null) {
            for (DataMap map : project.getDataMaps()) {
                for (ObjEntity entity : map.getObjEntities()) {
                    for (ObjRelationship objRelationship : entity.getRelationships()) {
                        if (objRelationship.getDbRelationships().contains(relationship)) {
                            objRelationships.add(objRelationship);
                        }
                    }
                }
            }
        }
        return objRelationships;
    }

    public static Collection<ObjAttribute> objAttributesUsingDbRelationship(
            Project project,
            DbRelationship relationship) {

        List<ObjAttribute> attributes = new ArrayList<>();
        if (project != null) {
            for (DataMap map : project.getDataMaps()) {
                for (ObjEntity entity : map.getObjEntities()) {
                    for (ObjAttribute objAttribute : entity.getAttributes()) {
                        if (objAttribute.isFlattened()) {
                            objAttribute.getDbPathIterator().forEachRemaining(entry -> {
                                if (entry instanceof DbRelationship) {
                                    if (entry.equals(relationship)) {
                                        attributes.add(objAttribute);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
        return attributes;
    }

    /**
     * Returns true if every join connects a PK column of the source to a PK column of the target. The join
     * attributes are resolved by name against the given entities, so this also works for joins not yet committed to
     * a relationship.
     */
    public static boolean isPkToPk(List<DbJoin> joins, DbEntity source, DbEntity target) {
        if (joins.isEmpty()) {
            return false;
        }

        for (DbJoin join : joins) {
            DbAttribute sourceAttribute = source != null && join.getSourceName() != null
                    ? source.getAttribute(join.getSourceName())
                    : null;
            DbAttribute targetAttribute = target != null && join.getTargetName() != null
                    ? target.getAttribute(join.getTargetName())
                    : null;

            if (targetAttribute != null && !targetAttribute.isPrimaryKey()
                    || sourceAttribute != null && !sourceAttribute.isPrimaryKey()) {
                return false;
            }
        }

        return true;
    }
}

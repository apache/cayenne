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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class DbAttributeOps {

    public static Collection<DbRelationship> relationshipsUsingAttribute(DbAttribute attribute) {
        DbEntity parent = attribute.getEntity();
        if (parent == null) {
            return Collections.emptyList();
        }

        DataMap map = parent.getDataMap();
        Iterable<DbEntity> entities = (map != null) ? map.getDbEntities() : Collections.singleton(parent);

        Collection<DbRelationship> relationships = new ArrayList<>();
        for (DbEntity entity : entities) {
            for (DbRelationship relationship : entity.getRelationships()) {
                for (DbJoin join : relationship.getJoins()) {
                    if (join.getSource() == attribute || join.getTarget() == attribute) {
                        relationships.add(relationship);
                        break;
                    }
                }
            }
        }
        return relationships;
    }
}

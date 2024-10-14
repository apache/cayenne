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
package org.apache.cayenne.dbsync.merge.builders;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

/**
 * @since 4.0.
 */
public class DbRelationshipBuilder extends DefaultBuilder<DbRelationship> {

    private String[] from;
    private String[] to;

    public DbRelationshipBuilder() {
        super(new DbRelationship());
    }

    public DbRelationshipBuilder(String name) {
        super(new DbRelationship(name));
    }

    public DbRelationshipBuilder(DbRelationship obj) {
        super(obj);
    }

    public DbRelationshipBuilder name() {
        return name(getRandomJavaName());
    }

    public DbRelationshipBuilder name(String name) {
        obj.setName(name);

        return this;
    }

    public DbRelationshipBuilder from(DbEntity entity, String... columns) {
        obj.setSourceEntity(entity);
        this.from = columns;

        return this;
    }

    public DbRelationshipBuilder fK(boolean fk) {
        obj.setFK(fk);
        return this;
    }

    public DbRelationshipBuilder to(String entityName, String... columns) {
        obj.setTargetEntityName(entityName);
        this.to = columns;

        return this;
    }

    @Override
    public DbRelationship build() {
        if (obj.getName() == null) {
            name();
        }

        if (from.length != to.length) {
            throw new IllegalStateException("from and to columns name size mismatch");
        }

        for (int i = 0; i < from.length; i++) {
            obj.addJoin(new DbJoin(obj, from[i], to[i]));
        }

        DbJoin join = new DbJoin(obj);
        if (!obj.isFK() && join.getTarget() != null && join.getSource() != null) {
            if (join.getTarget().isPrimaryKey() && !join.getSource().isPrimaryKey()) {
                obj.setFK(true);
            }
        }

        return obj;
    }
}

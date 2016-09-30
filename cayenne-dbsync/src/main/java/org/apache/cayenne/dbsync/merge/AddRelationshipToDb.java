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
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

import java.util.Collections;
import java.util.List;

public class AddRelationshipToDb extends AbstractToDbToken.Entity {

    private DbRelationship relationship;

    public AddRelationshipToDb(DbEntity entity, DbRelationship relationship) {
        super("Add foreign key", entity);
        this.relationship = relationship;
    }

    /**
     * @see DbGenerator#createConstraintsQueries(org.apache.cayenne.map.DbEntity)
     */
    @Override
    public List<String> createSql(DbAdapter adapter) {
        // TODO: skip FK to a different DB

        if (this.shouldGenerateFkConstraint()) {
            String fksql = adapter.createFkConstraint(relationship);
            if (fksql != null) {
                return Collections.singletonList(fksql);
            }
        }
        return Collections.emptyList();
    }

    public boolean shouldGenerateFkConstraint() {
        return !relationship.isToMany()
                && relationship.isToPK() // TODO it is not necessary primary key it can be unique index
                && !relationship.isToDependentPK();
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createDropRelationshipToModel(getEntity(), relationship);
    }

    @Override
    public String getTokenValue() {
        if (this.shouldGenerateFkConstraint()) {
            return relationship.getSourceEntity().getName() + "->" + relationship.getTargetEntityName();
        } else {
            return "Skip. No sql representation.";
        }
    }
    
    @Override
    public int compareTo(MergerToken o) {
        // add all AddRelationshipToDb to the end.
        if (o instanceof AddRelationshipToDb) {
            return super.compareTo(o);
        }
        return 1;
    }

}

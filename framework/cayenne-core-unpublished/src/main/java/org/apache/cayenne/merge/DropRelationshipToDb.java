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

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DbRelationshipDetected;

public class DropRelationshipToDb extends AbstractToDbToken.Entity {

    private DbRelationship rel;

    public DropRelationshipToDb(DbEntity entity, DbRelationship rel) {
        super(entity);
        this.rel = rel;
    }
    
    public String getFkName() {
        if (rel instanceof DbRelationshipDetected) {
            return ((DbRelationshipDetected) rel).getFkName();
        }
        return null;
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        String fkName = getFkName();
        
        if (fkName == null) {
            return Collections.emptyList();
        }
        QuotingStrategy context = adapter.getQuotingStrategy();
        StringBuilder buf = new StringBuilder();
        buf.append("ALTER TABLE ");
        buf.append(context.quotedFullyQualifiedName(getEntity()));
        buf.append(" DROP CONSTRAINT ");
        buf.append(fkName);

        return Collections.singletonList(buf.toString());
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createAddRelationshipToModel(getEntity(), rel);
    }

    public String getTokenName() {
        return "Drop Relationship";
    }

    @Override
    public String getTokenValue() {
        StringBuilder s = new StringBuilder();
        s.append(rel.getSourceEntity().getName());
        s.append("->");
        s.append(rel.getTargetEntityName());
        return s.toString();
    }
    
    public DbRelationship getRelationship() {
        return rel;
    }

}

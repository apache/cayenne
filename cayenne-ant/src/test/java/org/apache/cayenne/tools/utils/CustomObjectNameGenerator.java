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

package org.apache.cayenne.tools.utils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.cayenne.dbsync.naming.DbEntityNameStemmer;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.Util;
import org.jvnet.inflector.Noun;

public class CustomObjectNameGenerator implements ObjectNameGenerator {

    private DbEntityNameStemmer dbEntityNameStemmer;

    public CustomObjectNameGenerator() {
        this.dbEntityNameStemmer = NoStemStemmer.getInstance();
    }

    public CustomObjectNameGenerator(DbEntityNameStemmer dbEntityNameStemmer) {
        this.dbEntityNameStemmer = dbEntityNameStemmer;
    }

    @Override
    public String relationshipName(DbRelationship... relationshipChain) {

        if (relationshipChain == null || relationshipChain.length < 1) {
            throw new IllegalArgumentException("At least on relationship is expected: " + relationshipChain);
        }

        // ignore the name of DbRelationship itself (FWIW we may be generating a new name for it here)...
        // generate the name based on join semantics...

        String name = isToMany(relationshipChain)
                ? toManyRelationshipName(relationshipChain)
                : toOneRelationshipName(relationshipChain);

        return Util.underscoredToJava(name, false);
    }

    protected boolean isToMany(DbRelationship... relationshipChain) {

        for (DbRelationship r : relationshipChain) {
            if (r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    protected String stemmed(String dbEntityName) {
        return dbEntityNameStemmer.stem(Objects.requireNonNull(dbEntityName));
    }

    protected String toManyRelationshipName(DbRelationship... relationshipChain) {

        DbRelationship last = relationshipChain[relationshipChain.length - 1];

        String baseName = stemmed(last.getTargetEntityName());

        try {
            // by default we use English rules here...
            return Noun.pluralOf(baseName.toLowerCase(), Locale.ENGLISH);
        } catch (Exception inflectorError) {
            //  seems that Inflector cannot be trusted. For instance, it
            // throws an exception when invoked for word "ADDRESS" (although
            // lower case works fine). To feel safe, we use superclass'
            // behavior if something's gone wrong
            return baseName;
        }
    }

    protected String toOneRelationshipName(DbRelationship... relationshipChain) {

        DbRelationship first = relationshipChain[0];
        DbRelationship last = relationshipChain[relationshipChain.length - 1];

        List<DbJoin> joins = first.getJoins();
        if (joins.isEmpty()) {
            // In case, when uses EditRelationship button, relationship doesn't exist => it doesn't have joins
            // and just return targetName
            return stemmed(last.getTargetEntityName());
        }

        DbJoin join1 = joins.get(0);

        // TODO: multi-join relationships

        // return the name of the FK column sans ID
        String fkColName = join1.getSourceName();
        if (fkColName == null) {
            return stemmed(last.getTargetEntityName());
        } else if (fkColName.toUpperCase().endsWith("_ID") && fkColName.length() > 3) {
            return fkColName.substring(0, fkColName.length() - 3);
        } else if (fkColName.toUpperCase().endsWith("ID") && fkColName.length() > 2) {
            return fkColName.substring(0, fkColName.length() - 2);
        } else {
            return stemmed(last.getTargetEntityName());
        }
    }

    @Override
    public String objEntityName(DbEntity dbEntity) {
        String baseName = stemmed(dbEntity.getName());
        return Util.underscoredToJava(baseName, true);
    }

    @Override
    public String objAttributeName(DbAttribute attr) {
        return Util.underscoredToJava(attr.getName(), false);
    }
}

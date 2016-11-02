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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.dbsync.reverse.db.ExportedKey;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.Util;
import org.jvnet.inflector.Noun;

import java.util.Locale;

/**
 * The default strategy for converting DB-layer to Object-layer names.
 *
 * @since 4.0
 */
public class DefaultObjectNameGenerator implements ObjectNameGenerator {


    @Override
    public String dbRelationshipName(ExportedKey key, boolean toMany) {
        String name = toMany ? toManyRelationshipName(key) : toOneRelationshipName(key);
        return Util.underscoredToJava(name, false);
    }

    protected String toManyRelationshipName(ExportedKey key) {
        try {
            // by default we use English rules here...
            return Noun.pluralOf(key.getFKTableName().toLowerCase(), Locale.ENGLISH);
        } catch (Exception inflectorError) {
            //  seems that Inflector cannot be trusted. For instance, it
            // throws an exception when invoked for word "ADDRESS" (although
            // lower case works fine). To feel safe, we use superclass'
            // behavior if something's gone wrong
            return key.getFKTableName();
        }
    }

    protected String toOneRelationshipName(ExportedKey key) {
        String fkColName = key.getFKColumnName();

        if (fkColName == null) {
            return key.getPKTableName();
        }
        // trim "ID" in the end
        else if (fkColName.toUpperCase().endsWith("_ID") && fkColName.length() > 3) {
            return fkColName.substring(0, fkColName.length() - 3);
        } else if (fkColName.toUpperCase().endsWith("ID") && fkColName.length() > 2) {
            return fkColName.substring(0, fkColName.length() - 2);
        } else {
            return key.getPKTableName();
        }
    }

    @Override
    public String objEntityName(DbEntity dbEntity) {
        return Util.underscoredToJava(dbEntity.getName(), true);
    }

    @Override
    public String objAttributeName(DbAttribute attr) {
        return Util.underscoredToJava(attr.getName(), false);
    }

    @Override
    public String objRelationshipName(DbRelationship dbRel) {
        return Util.underscoredToJava(dbRel.getName(), false);
    }
}

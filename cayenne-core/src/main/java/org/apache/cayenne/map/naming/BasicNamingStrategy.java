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
package org.apache.cayenne.map.naming;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.NameConverter;

/**
 * BasicNamingStrategy is an naming strategy that creates names in Cayenne's
 * old-fashioned manner, i.e. the same way Cayenne did before 3.0
 * 
 * @since 3.0
 */
public class BasicNamingStrategy implements NamingStrategy {
    public String createDbRelationshipName(
            ExportedKey key,
            boolean toMany) {
        
        String uglyName = (toMany) ? key.getFKTableName() + "_ARRAY" : "to_" + key.getPKTableName();
        return NameConverter.underscoredToJava(uglyName, false);
    }

    public String createObjEntityName(DbEntity dbEntity) {
        return NameConverter.underscoredToJava(dbEntity.getName(), true);
    }

    public String createObjAttributeName(DbAttribute attr) {
        return NameConverter.underscoredToJava(attr.getName(), false);
    }

    public String createObjRelationshipName(DbRelationship dbRel) {
        return NameConverter.underscoredToJava(dbRel.getName(), false);
    }
}

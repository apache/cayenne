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

/**
 * An ObjectNameGenerator that creates names in Cayenne's old-fashioned style. I.e. the same way Cayenne did before 3.0,
 * with "to" prefixes and "array" suffixes.
 *
 * @since 4.0
 */
public class LegacyObjectNameGenerator implements ObjectNameGenerator {

    @Override
    public String dbRelationshipName(
            ExportedKey key,
            boolean toMany) {

        String uglyName = (toMany) ? key.getFKTableName() + "_ARRAY" : "to_" + key.getPKTableName();
        return Util.underscoredToJava(uglyName, false);
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

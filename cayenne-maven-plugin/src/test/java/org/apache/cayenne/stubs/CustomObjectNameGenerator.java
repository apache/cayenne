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

package org.apache.cayenne.stubs;

import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.Util;

import java.util.List;

public class CustomObjectNameGenerator implements ObjectNameGenerator {

    @Override
    public String objRelationshipName(DbRelationship... relationshipChain) {
        return null;
    }

    @Override
    public String dbRelationshipName(List<DbJoin> joins, boolean toMany) {
        return null;
    }

    @Override
    public String objEntityName(DbEntity dbEntity) {
        return "Custom" + Util.underscoredToJava(dbEntity.getName(), true);
    }

    @Override
    public String objAttributeName(DbAttribute attr) {
        return "custom_" + Util.underscoredToJava(attr.getName(), false);
    }
}

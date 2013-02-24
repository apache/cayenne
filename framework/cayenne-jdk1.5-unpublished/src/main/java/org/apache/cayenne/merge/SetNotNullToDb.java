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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * A {@link MergerToken} to add a "not null" clause to a column.
 */
public class SetNotNullToDb extends AbstractToDbToken.EntityAndColumn {

    public SetNotNullToDb(DbEntity entity, DbAttribute column) {
        super(entity, column);
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        StringBuilder sqlBuffer = new StringBuilder();
        QuotingStrategy context = adapter.getQuotingStrategy();
        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
        sqlBuffer.append(" ALTER COLUMN ");
        sqlBuffer.append(context.quotedName(getColumn()));
        sqlBuffer.append(" SET NOT NULL");

        return Collections.singletonList(sqlBuffer.toString());
    }

    public String getTokenName() {
        return "Set Not Null";
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createSetAllowNullToModel(getEntity(), getColumn());
    }

}

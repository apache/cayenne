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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.jdbc.ParameterBinding;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class DefaultValueForNullProvider implements ValueForNullProvider {

    private Map<String, ParameterBinding> values = new HashMap<String, ParameterBinding>();

    public void set(DbEntity entity, DbAttribute column, Object value, int type) {
        values.put(createKey(entity, column), new ParameterBinding(value, type, column
                .getAttributePrecision()));
    }

    protected ParameterBinding get(DbEntity entity, DbAttribute column) {
        return values.get(createKey(entity, column));
    }

    public List<String> createSql(DbEntity entity, DbAttribute column) {
        ParameterBinding value = get(entity, column);
        if (value == null) {
            return Collections.emptyList();
        }

        // TODO: change things so it is possible to use prepared statements here
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(entity.getFullyQualifiedName());
        sql.append(" SET ");
        sql.append(column.getName());
        sql.append("='");
        sql.append(value.getValue());
        sql.append("' WHERE ");
        sql.append(column.getName());
        sql.append(" IS NULL");
        return Collections.singletonList(sql.toString());
    }

    public boolean hasValueFor(DbEntity entity, DbAttribute column) {
        return values.containsKey(createKey(entity, column));
    }

    private String createKey(DbEntity entity, DbAttribute attribute) {
        return (entity.getFullyQualifiedName() + "." + attribute.getName()).toUpperCase();
    }

}

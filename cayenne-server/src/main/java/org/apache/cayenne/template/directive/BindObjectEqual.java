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

package org.apache.cayenne.template.directive;

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.template.Context;
import org.apache.cayenne.template.parser.ASTExpression;

/**
 * @since 4.1
 */
public class BindObjectEqual implements Directive {

    public static final BindObjectEqual INSTANCE = new BindObjectEqual();

    @Override
    public void apply(Context context, ASTExpression... expressions) {

        Object object = expressions[0].evaluateAsObject(context);
        Map<String, Object> idMap = toIdMap(object);

        Object sqlColumns = null;
        Object idColumns = null;
        if(expressions.length > 1) {
            sqlColumns = expressions[1].evaluateAsObject(context);
        }
        if(expressions.length > 2) {
            idColumns = expressions[2].evaluateAsObject(context);
        }

        if (idMap == null) {
            // assume null object, and bind all null values
            if (sqlColumns == null || idColumns == null) {
                throw new CayenneRuntimeException("Invalid parameters. "
                        + "Either object has to be set or sqlColumns and idColumns or both.");
            }

            idMap = Collections.emptyMap();
        } else if (sqlColumns == null || idColumns == null) {
            // infer SQL columns from ID columns
            sqlColumns = idMap.keySet().toArray();
            idColumns = sqlColumns;
        }

        String[] sqlColumnsArray = toArray(sqlColumns);
        String[] idColumnsArray = toArray(idColumns);

        if (sqlColumnsArray.length != idColumnsArray.length) {
            throw new CayenneRuntimeException(
                    "SQL columns and ID columns arrays have different sizes.");
        }

        for (int i = 0; i < sqlColumnsArray.length; i++) {
            Object value = idMap.get(idColumnsArray[i]);
            int jdbcType = (value != null) ? TypesMapping.getSqlTypeByJava(value.getClass()) : Types.INTEGER;

            renderColumn(context, sqlColumnsArray[i], i);
            render(context, new ParameterBinding(value, jdbcType, -1));
        }
    }

    protected void renderColumn(Context context, String columnName, int columnIndex) {
        if (columnIndex > 0) {
            context.getBuilder().append(" AND ");
        }

        context.getBuilder().append(columnName).append(' ');
    }

    protected void render(Context context, ParameterBinding binding) {
        if (binding.getValue() != null) {
            context.addParameterBinding(binding);
            context.getBuilder().append("= ?");
        } else {
            context.getBuilder().append("IS NULL");
        }
    }

    @SuppressWarnings("unchecked")
    protected String[] toArray(Object columns) {
        if (columns instanceof Collection) {
            String[] columnsAsStrings = new String[((Collection<Object>)columns).size()];
            int idx = 0;
            for(Object column : (Collection<Object>)columns) {
                columnsAsStrings[idx++] = column.toString();
            }
            return columnsAsStrings;
        } else if (columns.getClass().isArray()) {
            String[] columnsAsStrings = new String[((Object[])columns).length];
            int idx = 0;
            for(Object column : (Object[])columns) {
                columnsAsStrings[idx++] = column.toString();
            }
            return columnsAsStrings;
        } else {
            return new String[] { columns.toString() };
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> toIdMap(Object object) {
        if (object instanceof Persistent) {
            return ((Persistent) object).getObjectId().getIdSnapshot();
        } else if (object instanceof ObjectId) {
            return ((ObjectId) object).getIdSnapshot();
        } else if(object instanceof Map) {
            return (Map<String, Object>) object;
        } else if (object != null) {
            throw new CayenneRuntimeException(
                    "Invalid object parameter, expected Persistent or ObjectId or null: " + object);
        } else {
            return null;
        }
    }
}

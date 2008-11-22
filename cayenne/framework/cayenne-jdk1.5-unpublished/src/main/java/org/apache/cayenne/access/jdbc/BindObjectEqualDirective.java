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
package org.apache.cayenne.access.jdbc;

import java.io.IOException;
import java.io.Writer;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * A custom Velocity directive to create a set of SQL conditions to match an ObjectId of
 * an object. Usage in Velocity template is "WHERE #bindObjectEqual($object)" or "WHERE
 * #bindObjectEqual($object $columns $idValues)".
 * 
 * @since 3.0
 */
public class BindObjectEqualDirective extends BindDirective {

    @Override
    public String getName() {
        return "bindObjectEqual";
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException,
            MethodInvocationException {

        Object object = getChild(context, node, 0);
        Map idMap = toIdMap(object);

        Object sqlColumns = getChild(context, node, 1);
        Object idColumns = getChild(context, node, 2);

        if (idMap == null) {
            // assume null object, and bind all null values

            if (sqlColumns == null || idColumns == null) {
                throw new ParseErrorException("Invalid parameters. "
                        + "Either object has to be set "
                        + "or sqlColumns and idColumns or both.");
            }

            idMap = Collections.EMPTY_MAP;
        }
        else if (sqlColumns == null || idColumns == null) {
            // infer SQL columns from ID columns
            sqlColumns = idMap.keySet().toArray();
            idColumns = sqlColumns;
        }

        Object[] sqlColumnsArray = toArray(sqlColumns);
        Object[] idColumnsArray = toArray(idColumns);

        if (sqlColumnsArray.length != idColumnsArray.length) {
            throw new ParseErrorException(
                    "SQL columns and ID columns arrays have different sizes.");
        }

        for (int i = 0; i < sqlColumnsArray.length; i++) {

            Object value = idMap.get(idColumnsArray[i]);

            int jdbcType = (value != null) ? TypesMapping.getSqlTypeByJava(value
                    .getClass()) : Types.INTEGER;

            renderColumn(context, writer, sqlColumnsArray[i], i);
            writer.write(' ');
            render(context, writer, new ParameterBinding(value, jdbcType, -1));
        }

        return true;
    }

    protected Object[] toArray(Object columns) {
        if (columns instanceof Collection) {
            return ((Collection) columns).toArray();
        }
        else if (columns.getClass().isArray()) {
            return (Object[]) columns;
        }
        else {
            return new Object[] {
                columns
            };
        }
    }

    protected Map toIdMap(Object object) throws ParseErrorException {
        if (object instanceof Persistent) {
            return ((Persistent) object).getObjectId().getIdSnapshot();
        }
        else if (object instanceof ObjectId) {
            return ((ObjectId) object).getIdSnapshot();
        }
        else if(object instanceof Map) {
            return (Map) object;
        }
        else if (object != null) {
            throw new ParseErrorException(
                    "Invalid object parameter, expected Persistent or ObjectId or null: "
                            + object);
        }
        else {
            return null;
        }
    }

    protected void renderColumn(
            InternalContextAdapter context,
            Writer writer,
            Object columnName,
            int columnIndex) throws IOException {

        if (columnIndex > 0) {
            writer.write(" AND ");
        }

        writer.write(columnName.toString());
    }

    @Override
    protected void render(
            InternalContextAdapter context,
            Writer writer,
            ParameterBinding binding) throws IOException {

        if (binding.getValue() != null) {
            bind(context, binding);
            writer.write("= ?");
        }
        else {
            writer.write("IS NULL");
        }
    }
}

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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.cayenne.util.Util;

/**
 * A custom Velocity directive to describe a ResultSet column. There are the following
 * possible invocation formats inside the template:
 * 
 * <pre>
 *       #result(column_name) - e.g. #result('ARTIST_ID')
 *       #result(column_name java_type) - e.g. #result('ARTIST_ID' 'String')
 *       #result(column_name java_type column_alias) - e.g. #result('ARTIST_ID' 'String' 'ID')
 *       #result(column_name java_type column_alias data_row_key) - e.g. #result('ARTIST_ID' 'String' 'ID' 'toArtist.ID')
 * </pre>
 * 
 * <p>
 * 'data_row_key' is needed if SQL 'column_alias' is not appropriate as a DataRow key on
 * the Cayenne side. One common case when this happens is when a DataRow retrieved from a
 * query is mapped using joint prefetch keys. In this case DataRow must use DB_PATH
 * expressions for joint column keys, and their format is incompatible with most databases
 * alias format.
 * </p>
 * <p>
 * Most common Java types used in JDBC can be specified without a package. This includes
 * all numeric types, primitives, String, SQL dates, BigDecimal and BigInteger.
 * </p>
 * 
 * @since 1.1
 */
public class ResultDirective extends Directive {

    private static final Map<String, String> typesGuess;

    static {
        // init default types
        typesGuess = new HashMap<String, String>();

        // primitives
        typesGuess.put("long", Long.class.getName());
        typesGuess.put("double", Double.class.getName());
        typesGuess.put("byte", Byte.class.getName());
        typesGuess.put("boolean", Boolean.class.getName());
        typesGuess.put("float", Float.class.getName());
        typesGuess.put("short", Short.class.getName());
        typesGuess.put("int", Integer.class.getName());

        // numeric
        typesGuess.put("Long", Long.class.getName());
        typesGuess.put("Double", Double.class.getName());
        typesGuess.put("Byte", Byte.class.getName());
        typesGuess.put("Boolean", Boolean.class.getName());
        typesGuess.put("Float", Float.class.getName());
        typesGuess.put("Short", Short.class.getName());
        typesGuess.put("Integer", Integer.class.getName());

        // other
        typesGuess.put("String", String.class.getName());
        typesGuess.put("Date", Date.class.getName());
        typesGuess.put("Time", Time.class.getName());
        typesGuess.put("Timestamp", Timestamp.class.getName());
        typesGuess.put("BigDecimal", BigDecimal.class.getName());
        typesGuess.put("BigInteger", BigInteger.class.getName());
    }

    @Override
    public String getName() {
        return "result";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException,
            MethodInvocationException {

        String column = getChildAsString(context, node, 0);
        if (column == null) {
            throw new ParseErrorException("Column name expected at line "
                    + node.getLine()
                    + ", column "
                    + node.getColumn());
        }

        String alias = getChildAsString(context, node, 2);
        String dataRowKey = getChildAsString(context, node, 3);

        // determine what we want to name this column in a resulting DataRow...
        String label = (!Util.isEmptyString(dataRowKey)) ? dataRowKey : (!Util
                .isEmptyString(alias)) ? alias : null;

        ColumnDescriptor columnDescriptor = new ColumnDescriptor();
        columnDescriptor.setName(column);
        columnDescriptor.setDataRowKey(label);

        String type = getChildAsString(context, node, 1);
        if (type != null) {
            columnDescriptor.setJavaClass(guessType(type));
        }

        // TODO: andrus 6/27/2007 - this is an unofficial jdbcType parameter that is added
        // temporarily pending CAY-813 implementation for the sake of EJBQL query...
        Object jdbcType = getChild(context, node, 4);
        if (jdbcType instanceof Number) {
            columnDescriptor.setJdbcType(((Number) jdbcType).intValue());
        }

        writer.write(column);

        // append column alias if needed.

        // Note that if table aliases are used, this logic will result in SQL like
        // "t0.ARTIST_NAME AS ARTIST_NAME". Doing extra regex matching to handle this
        // won't probably buy us much.
        if (!Util.isEmptyString(alias) && !alias.equals(column)) {
            writer.write(" AS ");
            writer.write(alias);
        }

        bindResult(context, columnDescriptor);
        return true;
    }

    protected Object getChild(InternalContextAdapter context, Node node, int i)
            throws MethodInvocationException {
        return (i >= 0 && i < node.jjtGetNumChildren()) ? node.jjtGetChild(i).value(
                context) : null;
    }

    /**
     * Returns a directive argument at a given index converted to String.
     * 
     * @since 1.2
     */
    protected String getChildAsString(InternalContextAdapter context, Node node, int i)
            throws MethodInvocationException {
        Object value = getChild(context, node, i);
        return (value != null) ? value.toString() : null;
    }

    /**
     * Converts "short" type notation to the fully qualified class name. Right now
     * supports all major standard SQL types, including primitives. All other types are
     * expected to be fully qualified, and are not converted.
     */
    protected String guessType(String type) {
        String guessed = typesGuess.get(type);
        return guessed != null ? guessed : type;
    }

    /**
     * Adds value to the list of result columns in the context.
     */
    protected void bindResult(
            InternalContextAdapter context,
            ColumnDescriptor columnDescriptor) {

        Collection<Object> resultColumns = (Collection<Object>) context
                .getInternalUserContext()
                .get(SQLTemplateProcessor.RESULT_COLUMNS_LIST_KEY);

        if (resultColumns != null) {
            resultColumns.add(columnDescriptor);
        }
    }
}

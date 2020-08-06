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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.template.Context;
import org.apache.cayenne.template.parser.ASTExpression;
import org.apache.cayenne.util.Util;

/**
 * @since 4.1
 */
public class Result implements Directive {

    public static final Result INSTANCE = new Result();

    private static final Map<String, String> typesGuess;

    static {
        // init default types
        typesGuess = new HashMap<>();

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
    public void apply(Context context, ASTExpression... expressions) {

        ColumnDescriptor columnDescriptor = new ColumnDescriptor();

        String column = expressions[0].evaluateAsString(context);
        columnDescriptor.setName(column);

        if (expressions.length > 1) {
            String type = expressions[1].evaluateAsString(context);
            columnDescriptor.setJavaClass(guessType(type));
        }

        String alias = null;
        if (expressions.length > 2) {
            alias = expressions[2].evaluateAsString(context);
        }

        String dataRowKey = null;
        if (expressions.length > 3) {
            dataRowKey = expressions[3].evaluateAsString(context);
        }

        // determine what we want to name this column in a resulting DataRow...
        String label = (!Util.isEmptyString(dataRowKey)) ? dataRowKey : (!Util.isEmptyString(alias)) ? alias : null;
        columnDescriptor.setDataRowKey(label);

        if (expressions.length > 4) {
            int jdbcType = (int) expressions[4].evaluateAsLong(context);
            columnDescriptor.setJdbcType(jdbcType);
        }

        context.addColumnDescriptor(columnDescriptor);

        context.getBuilder().append(column);
        if (!Util.isEmptyString(alias) && !alias.equals(column)) {
            context.getBuilder().append(" AS ").append(alias);
        }
    }

    /**
     * Converts "short" type notation to the fully qualified class name. Right
     * now supports all major standard SQL types, including primitives. All
     * other types are expected to be fully qualified, and are not converted.
     */
    protected String guessType(String type) {
        String guessed = typesGuess.get(type);
        return guessed != null ? guessed : type;
    }
}

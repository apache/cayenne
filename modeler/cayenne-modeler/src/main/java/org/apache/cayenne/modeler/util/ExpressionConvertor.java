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

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.parser.ParseException;
import org.apache.cayenne.util.Util;

/**
 * A Scope convertor that allows to display expressions in text fields.
 * 
 * @since 1.1
 */
public class ExpressionConvertor {

    public String valueAsString(Object value) throws IllegalArgumentException {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Expression)) {
            throw new IllegalArgumentException(
                "Unsupported value class: " + value.getClass().getName());
        }

        return value.toString();
    }

    public Object stringAsValue(String string) throws IllegalArgumentException {
        if (string == null || string.trim().length() == 0) {
            return null;
        }

        try {
            return Expression.fromString(string);
        }
        catch (ExpressionException eex) {
            // this is likely a parse exception... show detailed message
            Throwable cause = Util.unwindException(eex);
            String message =
                (cause instanceof ParseException)
                    ? cause.getMessage()
                    : "Invalid expression: " + string;

            throw new IllegalArgumentException(message);
        }
    }

    public boolean supportsStringAsValue() {
        return true;
    }
}

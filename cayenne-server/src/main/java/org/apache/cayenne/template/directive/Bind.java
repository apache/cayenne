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

package org.apache.cayenne.template.directive;

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.template.Context;
import org.apache.cayenne.template.Directive;
import org.apache.cayenne.template.parser.ASTExpression;

/**
 * @since 4.1
 */
public class Bind implements Directive {

    @Override
    public String apply(Context context, ASTExpression... expressions) {
        if(expressions.length < 2) {
            throw new IllegalArgumentException();
        }

        Object value = expressions[0].evaluateAsObject(context);
        String jdbcTypeName = expressions[1].evaluate(context);
        int jdbcType;
        if (jdbcTypeName != null) {
            jdbcType = TypesMapping.getSqlTypeByName(jdbcTypeName);
        } else if (value != null) {
            jdbcType = TypesMapping.getSqlTypeByJava(value.getClass());
        } else {
            jdbcType = TypesMapping.getSqlTypeByName(TypesMapping.SQL_NULL);
        }
        int scale = expressions.length < 3 ? -1 : (int)expressions[2].evaluateAsLong(context);

        ParameterBinding binding = new ParameterBinding(value, jdbcType, scale);
        context.addParameterBinding(binding);

        return "?";
    }
}

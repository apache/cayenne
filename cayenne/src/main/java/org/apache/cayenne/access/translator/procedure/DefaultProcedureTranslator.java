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

package org.apache.cayenne.access.translator.procedure;

import org.apache.cayenne.access.jdbc.CSParameter;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.TranslatedProcedure;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.compiled.CompiledProcedure;
import org.apache.cayenne.map.compiled.CompiledProcedureParameter;
import org.apache.cayenne.query.resolved.ResolvedProcedure;

import java.util.Map;

/**
 * A {@link ProcedureTranslator} returned by the base {@link org.apache.cayenne.dba.JdbcAdapter}. Adapters may subclass
 * it to customize the generated call string.
 *
 * @since 5.0
 */
public class DefaultProcedureTranslator implements ProcedureTranslator {

    /**
     * Helper class to make OUT and VOID parameters logger-friendly.
     */
    static class NotInParam {

        protected String type;

        public NotInParam(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    private static final NotInParam OUT_PARAM = new NotInParam("[OUT]");

    @Override
    public TranslatedProcedure translate(ResolvedProcedure query, DbAdapter adapter) {

        CompiledProcedure procedure = query.procedure();
        CompiledProcedureParameter[] callParams = procedure.parameters().toArray(new CompiledProcedureParameter[0]);
        Map<String, ?> queryValues = query.params();

        CSParameter<?>[] params = new CSParameter[callParams.length];
        for (int i = 0; i < callParams.length; i++) {
            params[i] = createBinding(adapter, callParams[i], queryValues, i + 1);
        }

        String sql = createSqlString(procedure, callParams.length);
        return new TranslatedProcedure(query, sql, params);
    }

    /**
     * Builds a {@link CSParameter} for a single call parameter. IN (and INOUT) parameters carry the actual value
     * and its {@link ExtendedType}; pure OUT parameters carry an "[OUT]" marker value so that logging renders nicely.
     */
    protected CSParameter<?> createBinding(DbAdapter adapter, CompiledProcedureParameter param,
                                        Map<String, ?> queryValues, int position) {

        // match values with parameters in the correct order, assuming a missing value is NULL
        if (param.direction() == CompiledProcedureParameter.OUT_PARAMETER) {
            return new CSParameter<>(OUT_PARAM, position, param.jdbcType(), param.precision(), null, param);
        }

        Object value = queryValues.get(param.name());
        ExtendedType extendedType = value != null
                ? adapter.getExtendedTypes().getRegisteredType(value.getClass())
                : adapter.getExtendedTypes().getDefaultType();

        return new CSParameter<>(
                value, position, adapter.preferredBindingType(param.jdbcType()), param.precision(), extendedType,
                param);
    }

    /**
     * Creates an SQL String for the stored procedure call.
     */
    protected String createSqlString(CompiledProcedure procedure, int callParamsSize) {

        StringBuilder buf = new StringBuilder();

        int totalParams = callParamsSize;

        // check if procedure returns values
        if (procedure.returningValue()) {
            totalParams--;
            buf.append("{? = call ");
        } else {
            buf.append("{call ");
        }

        buf.append(procedure.fullyQualifiedName());

        if (totalParams > 0) {
            // unroll the loop
            buf.append("(?");

            for (int i = 1; i < totalParams; i++) {
                buf.append(", ?");
            }

            buf.append(")");
        }

        buf.append("}");
        return buf.toString();
    }
}

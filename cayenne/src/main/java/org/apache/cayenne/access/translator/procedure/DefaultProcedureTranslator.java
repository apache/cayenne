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

import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.ProcedureQuery;

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
    public TranslatedProcedure translate(ProcedureQuery query, DbAdapter adapter, EntityResolver resolver) {

        Procedure procedure = query.getMetaData(resolver).getProcedure();
        ProcedureParameter[] callParams = procedure.getCallParameters().toArray(new ProcedureParameter[0]);
        Map<String, ?> queryValues = query.getParameters();

        PSParameter[] bindings = new PSParameter[callParams.length];
        for (int i = 0; i < callParams.length; i++) {
            bindings[i] = createBinding(adapter, callParams[i], queryValues, i + 1);
        }

        String sql = createSqlString(procedure, callParams.length);
        return new TranslatedProcedure(sql, bindings, callParams);
    }

    /**
     * Builds a {@link PSParameter} for a single call parameter. IN (and INOUT) parameters carry the actual value
     * and its {@link ExtendedType}; pure OUT parameters carry an "[OUT]" marker value so that logging renders nicely.
     */
    protected PSParameter<?> createBinding(DbAdapter adapter, ProcedureParameter param,
                                        Map<String, ?> queryValues, int position) {

        // match values with parameters in the correct order, assuming a missing value is NULL
        if (param.getDirection() == ProcedureParameter.OUT_PARAMETER) {
            return new PSParameter<>(OUT_PARAM, position, param.getType(), param.getPrecision(), null, null);
        }

        Object value = queryValues.get(param.getName());
        ExtendedType extendedType = value != null
                ? adapter.getExtendedTypes().getRegisteredType(value.getClass())
                : adapter.getExtendedTypes().getDefaultType();

        return new PSParameter<>(
                value, position, adapter.preferredBindingType(param.getType()), param.getPrecision(), extendedType, null);
    }

    /**
     * Creates an SQL String for the stored procedure call.
     */
    protected String createSqlString(Procedure procedure, int callParamsSize) {

        StringBuilder buf = new StringBuilder();

        int totalParams = callParamsSize;

        // check if procedure returns values
        if (procedure.isReturningValue()) {
            totalParams--;
            buf.append("{? = call ");
        } else {
            buf.append("{call ");
        }

        buf.append(procedure.getFullyQualifiedName());

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

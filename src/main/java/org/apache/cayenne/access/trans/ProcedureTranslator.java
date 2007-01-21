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

package org.apache.cayenne.access.trans;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.QueryTranslator;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.ProcedureQuery;

/**
 * Stored procedure query translator.
 * 
 * @author Andrus Adamchik
 */
public class ProcedureTranslator extends QueryTranslator {

    /**
     * Helper class to make OUT and VOID parameters logger-friendly.
     */
    static class NotInParam {

        protected String type;

        public NotInParam(String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }
    }

    private static NotInParam OUT_PARAM = new NotInParam("[OUT]");

    protected List callParams;
    protected List values;

    /**
     * Creates an SQL String for the stored procedure call.
     */
    protected String createSqlString() {
        Procedure procedure = getProcedure();

        StringBuffer buf = new StringBuffer();

        int totalParams = callParams.size();

        // check if procedure returns values
        if (procedure.isReturningValue()) {
            totalParams--;
            buf.append("{? = call ");
        }
        else {
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

    public PreparedStatement createStatement() throws Exception {
        long t1 = System.currentTimeMillis();

        this.callParams = getProcedure().getCallParameters();
        this.values = new ArrayList(callParams.size());

        initValues();
        String sqlStr = createSqlString();

        if (QueryLogger.isLoggable()) {
            // need to convert OUT/VOID parameters to loggable strings
            long time = System.currentTimeMillis() - t1;

            List loggableParameters = new ArrayList(values.size());
            Iterator it = values.iterator();
            while (it.hasNext()) {
                Object val = it.next();
                if (val instanceof NotInParam) {
                    val = val.toString();
                }
                loggableParameters.add(val);
            }

            QueryLogger.logQuery(sqlStr, loggableParameters, time);
        }
        CallableStatement stmt = connection.prepareCall(sqlStr);
        initStatement(stmt);
        return stmt;
    }

    public Procedure getProcedure() {
        return getEntityResolver().lookupProcedure(query);
    }

    public ProcedureQuery getProcedureQuery() {
        return (ProcedureQuery) query;
    }

    /**
     * Set IN and OUT parameters.
     */
    protected void initStatement(CallableStatement stmt) throws Exception {
        if (values != null && values.size() > 0) {
            List params = getProcedure().getCallParameters();

            int len = values.size();
            for (int i = 0; i < len; i++) {
                ProcedureParameter param = (ProcedureParameter) params.get(i);

                // !Stored procedure parameter can be both in and out
                // at the same time
                if (param.isOutParam()) {
                    setOutParam(stmt, param, i + 1);
                }

                if (param.isInParameter()) {
                    setInParam(stmt, param, values.get(i), i + 1);
                }
            }
        }
    }

    protected void initValues() {
        Map queryValues = getProcedureQuery().getParameters();

        // match values with parameters in the correct order.
        // make an assumption that a missing value is NULL
        // Any reason why this is bad?

        Iterator it = callParams.iterator();
        while (it.hasNext()) {
            ProcedureParameter param = (ProcedureParameter) it.next();

            if (param.getDirection() == ProcedureParameter.OUT_PARAMETER) {
                values.add(OUT_PARAM);
            }
            else {
                values.add(queryValues.get(param.getName()));
            }
        }
    }

    /**
     * Sets a single IN parameter of the CallableStatement.
     */
    protected void setInParam(
            CallableStatement stmt,
            ProcedureParameter param,
            Object val,
            int pos) throws Exception {

        int type = param.getType();
        adapter.bindParameter(stmt, val, pos, type, param.getPrecision());
    }

    /**
     * Sets a single OUT parameter of the CallableStatement.
     */
    protected void setOutParam(CallableStatement stmt, ProcedureParameter param, int pos)
            throws Exception {

        int precision = param.getPrecision();
        if (precision >= 0) {
            stmt.registerOutParameter(pos, param.getType(), precision);
        }
        else {
            stmt.registerOutParameter(pos, param.getType());
        }
    }
}

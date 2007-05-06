/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.trans;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.query.ProcedureQuery;

/**
 * Stored procedure query translator.
 * 
 * @author Andrei Adamchik
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
     * Returns a result descriptor for the stored procedure OUT parameters.
     * 
     * @deprecated Since 1.2 is unused. Instead OUTParametersReader is created inside a
     *             SQLAction.
     */
    public org.objectstyle.cayenne.access.util.ResultDescriptor getProcedureResultDescriptor() {
        return org.objectstyle.cayenne.access.util.ResultDescriptor.createDescriptor(
                getProcedure(),
                getAdapter().getExtendedTypes());
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
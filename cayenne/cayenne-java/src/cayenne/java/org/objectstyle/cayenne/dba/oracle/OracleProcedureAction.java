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
package org.objectstyle.cayenne.dba.oracle;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.jdbc.ColumnDescriptor;
import org.objectstyle.cayenne.access.jdbc.ProcedureAction;
import org.objectstyle.cayenne.access.jdbc.RowDescriptor;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.query.ProcedureQuery;

/**
 * Oracle-specific ProcedureAction that supports ResultSet OUT parameters.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
class OracleProcedureAction extends ProcedureAction {

    OracleProcedureAction(ProcedureQuery query, DbAdapter adapter,
            EntityResolver entityResolver) {
        super(query, adapter, entityResolver);
    }

    /**
     * Helper method that reads OUT parameters of a CallableStatement.
     */
    protected void readProcedureOutParameters(
            CallableStatement statement,
            OperationObserver delegate) throws SQLException, Exception {

        long t1 = System.currentTimeMillis();

        // build result row...
        Map result = null;
        List parameters = getProcedure().getCallParameters();
        for (int i = 0; i < parameters.size(); i++) {
            ProcedureParameter parameter = (ProcedureParameter) parameters.get(i);

            if (!parameter.isOutParam()) {
                continue;
            }

            // ==== start Oracle-specific part
            if (parameter.getType() == OracleAdapter.getOracleCursorType()) {
                ResultSet rs = (ResultSet) statement.getObject(i + 1);

                try {
                    RowDescriptor rsDescriptor = describeResultSet(
                            rs,
                            processedResultSets++);
                    readResultSet(rs, rsDescriptor, query, delegate);
                }
                finally {
                    try {
                        rs.close();
                    }
                    catch (SQLException ex) {
                    }
                }
            }
            // ==== end Oracle-specific part
            else {
                if (result == null) {
                    result = new HashMap();
                }

                ColumnDescriptor descriptor = new ColumnDescriptor(parameter);
                ExtendedType type = getAdapter().getExtendedTypes().getRegisteredType(
                        descriptor.getJavaClass());
                Object val = type.materializeObject(statement, i + 1, descriptor
                        .getJdbcType());

                result.put(descriptor.getLabel(), val);
            }
        }

        if (result != null && !result.isEmpty()) {
            // treat out parameters as a separate data row set
            QueryLogger.logSelectCount(1, System.currentTimeMillis() - t1);
            delegate.nextDataRows(query, Collections.singletonList(result));
        }
    }
}
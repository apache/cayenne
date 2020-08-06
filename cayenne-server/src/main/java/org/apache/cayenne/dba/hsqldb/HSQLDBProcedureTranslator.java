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

package org.apache.cayenne.dba.hsqldb;

import org.apache.cayenne.access.translator.procedure.ProcedureTranslator;
import org.apache.cayenne.map.Procedure;

/**
 * Works around HSQLDB's pickiness about stored procedure syntax.
 * 
 * @since 1.2
 */
public class HSQLDBProcedureTranslator extends ProcedureTranslator {

    /**
     * Creates HSQLDB-compliant SQL to execute a stored procedure.
     */
    @Override
    protected String createSqlString() {
        Procedure procedure = getProcedure();

        StringBuilder buf = new StringBuilder();

        int totalParams = callParams.size();

        // check if procedure returns values
        if (procedure.isReturningValue()) {
            totalParams--;

            // HSQL won't accept "? =". The parser only recognizes "?="

            // TODO: Andrus, 12/12/2005 - this is kind of how it is in the
            // CallableStatement javadocs, so we may need to make "?=" a default ... this
            // requires testing on Oracle/PostgreSQL/Sybase/SQLServer.
            buf.append("{?= call ");
        }
        else {
            buf.append("{call ");
        }

        // HSQLDB requires that procedures with periods (referring to Java packages)
        // be enclosed in quotes. It is not clear that quotes can always be used, though
        if (procedure.getFullyQualifiedName().indexOf('.') > -1) {
            buf.append("\"").append(procedure.getFullyQualifiedName()).append("\"");
        }
        else {
            buf.append(procedure.getFullyQualifiedName());
        }

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

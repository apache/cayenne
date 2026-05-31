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

package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.access.translator.procedure.DefaultProcedureTranslator;
import org.apache.cayenne.map.Procedure;

/**
 * A translator that adds parenthesis to no-param queries.
 *
 * @since 5.0
 */
// see CAY-750 for the problem description
public class PostgresProcedureTranslator extends DefaultProcedureTranslator {

    @Override
    protected String createSqlString(Procedure procedure, int callParamsSize) {

        String sql = super.createSqlString(procedure, callParamsSize);

        // add empty parameter parenthesis
        if (sql.endsWith("}") && !sql.endsWith(")}")) {
            sql = sql.substring(0, sql.length() - 1) + "()}";
        }

        return sql;
    }
}

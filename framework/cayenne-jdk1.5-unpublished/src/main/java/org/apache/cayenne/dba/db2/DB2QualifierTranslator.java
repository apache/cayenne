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

package org.apache.cayenne.dba.db2;

import java.io.IOException;
import java.sql.Types;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.TrimmingQualifierTranslator;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;

/**
 */
public class DB2QualifierTranslator extends TrimmingQualifierTranslator {

    public DB2QualifierTranslator(QueryAssembler queryAssembler, String trimFunction) {
        super(queryAssembler, trimFunction);
    }

    @Override
    protected void appendLiteralDirect(
            Object val,
            DbAttribute attr,
            Expression parentExpression) throws IOException {

        boolean castNeeded = false;

        if (parentExpression != null) {
            int type = parentExpression.getType();

            castNeeded = attr != null
                    && (type == Expression.LIKE
                            || type == Expression.LIKE_IGNORE_CASE
                            || type == Expression.NOT_LIKE || type == Expression.NOT_LIKE_IGNORE_CASE);
        }

        if (castNeeded) {
            out.append("CAST (");
        }

        super.appendLiteralDirect(val, attr, parentExpression);

        if (castNeeded) {
            int jdbcType = attr.getType();
            int len = attr.getMaxLength();

            // determine CAST type

            // LIKE on CHAR may produce unpredictible results
            // LIKE on LONVARCHAR doesn't seem to be supported
            if (jdbcType == Types.CHAR || jdbcType == Types.LONGVARCHAR) {
                jdbcType = Types.VARCHAR;

                // length is required for VARCHAR
                if (len <= 0) {
                    len = 254;
                }
            }

            out.append(" AS ");
            String[] types = queryAssembler.getAdapter().externalTypesForJdbcType(
                    jdbcType);

            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException(
                        "Can't find database type for JDBC type '"
                                + TypesMapping.getSqlNameByType(jdbcType));
            }

            out.append(types[0]);
            if (len > 0 && TypesMapping.supportsLength(jdbcType)) {
                out.append("(");
                out.append(String.valueOf(len));
                out.append(")");
            }

            out.append(")");
        }
    }
}

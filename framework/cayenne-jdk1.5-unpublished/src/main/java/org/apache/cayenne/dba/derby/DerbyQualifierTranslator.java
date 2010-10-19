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
package org.apache.cayenne.dba.derby;

import java.io.IOException;
import java.sql.Types;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTNotEqual;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbAttribute;

public class DerbyQualifierTranslator extends TrimmingQualifierTranslator {

    public DerbyQualifierTranslator(QueryAssembler queryAssembler, String trimFunction) {
        super(queryAssembler, trimFunction);
    }

    @Override
    protected void processColumnWithQuoteSqlIdentifiers(
            DbAttribute dbAttr,
            Expression pathExp) throws IOException {

        SimpleNode parent = null;
        if (pathExp instanceof SimpleNode) {
            parent = (SimpleNode) ((SimpleNode) pathExp).jjtGetParent();
        }

        // problem in derby : Comparisons between 'CLOB (UCS_BASIC)' and 'CLOB (UCS_BASIC)' are not supported.
        // we need do it by casting the Clob to VARCHAR.
        if (parent != null
                && (parent instanceof ASTEqual || parent instanceof ASTNotEqual)
                && dbAttr.getType() == Types.CLOB
                && parent.getOperandCount() == 2
                && parent.getOperand(1) instanceof String) {
            Integer size = parent.getOperand(1).toString().length() + 1;

            out.append("CAST(");
            super.processColumnWithQuoteSqlIdentifiers(dbAttr, pathExp);
            out.append(" AS VARCHAR(" + size + "))");
        }
        else {
            super.processColumnWithQuoteSqlIdentifiers(dbAttr, pathExp);
        }
    }
}

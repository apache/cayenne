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

package org.apache.cayenne.dba.postgres;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.TrimmingColumnNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;
import org.apache.cayenne.dba.postgres.sqltree.PositionFunctionNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresExtractFunctionNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresLikeNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresLimitOffsetNode;

/**
 * @since 4.2
 */
public class PostgreSQLTreeProcessor extends BaseSQLTreeProcessor {

    private static final Set<String> EXTRACT_FUNCTION_NAMES = new HashSet<>(Arrays.asList(
        "DAY_OF_MONTH", "DAY", "MONTH", "HOUR", "WEEK", "YEAR", "DAY_OF_WEEK", "DAY_OF_YEAR", "MINUTE", "SECOND"
    ));

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        replaceChild(parent, index, new PostgresLimitOffsetNode(child), false);
    }

    @Override
    protected void onColumnNode(Node parent, ColumnNode child, int index) {
        replaceChild(parent, index, new TrimmingColumnNode(child));
    }

    @Override
    protected void onLikeNode(Node parent, LikeNode child, int index) {
        if(child.isIgnoreCase()) {
            replaceChild(parent, index, new PostgresLikeNode(child.isNot(), child.getEscape()));
        }
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        Node replacement = null;
        String functionName = child.getFunctionName();
        if(EXTRACT_FUNCTION_NAMES.contains(functionName)) {
            replacement = new PostgresExtractFunctionNode(functionName);
        } else if("CURRENT_DATE".equals(functionName)
                || "CURRENT_TIME".equals(functionName)
                || "CURRENT_TIMESTAMP".equals(functionName)) {
            replacement = new FunctionNode(functionName, child.getAlias(), false);
        } else if("LOCATE".equals(functionName)) {
            replacement = new PositionFunctionNode(child.getAlias());
        }

        if(replacement != null) {
            replaceChild(parent, index, replacement);
        }
    }

}

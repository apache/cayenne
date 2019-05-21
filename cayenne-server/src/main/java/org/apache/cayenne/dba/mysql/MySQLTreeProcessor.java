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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLikeNode;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLimitOffsetNode;

/**
 * @since 4.2
 */
public class MySQLTreeProcessor extends BaseSQLTreeProcessor {

    private static final MySQLTreeProcessor INSTANCE = new MySQLTreeProcessor();

    public static MySQLTreeProcessor getInstance() {
        return INSTANCE;
    }

    private MySQLTreeProcessor() {
    }

    @Override
    protected void onLikeNode(Node parent, LikeNode child, int index) {
        if(!child.isIgnoreCase()) {
            replaceChild(parent, index, new MysqlLikeNode(child.isNot(), child.getEscape()));
        }
    }

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        Node replacement = new MysqlLimitOffsetNode(child.getLimit(), child.getOffset());
        replaceChild(parent, index, replacement, false);
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        String functionName = child.getFunctionName();
        if("DAY_OF_MONTH".equals(functionName)
                || "DAY_OF_WEEK".equals(functionName)
                || "DAY_OF_YEAR".equals(functionName)) {
            FunctionNode replacement = new FunctionNode(functionName.replace("_", ""), child.getAlias(), true);
            replaceChild(parent, index, replacement);
        }
    }

}

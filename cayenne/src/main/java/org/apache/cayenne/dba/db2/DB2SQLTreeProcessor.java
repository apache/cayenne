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

package org.apache.cayenne.dba.db2;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.TrimmingColumnNode;
import org.apache.cayenne.dba.derby.sqltree.DerbyValueNode;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLimitOffsetNode;

/**
 * @since 4.2
 */
public class DB2SQLTreeProcessor extends BaseSQLTreeProcessor {


    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        Node replacement = new MysqlLimitOffsetNode(child.getLimit(), child.getOffset());
        replaceChild(parent, index, replacement, false);
    }

    @Override
    protected void onColumnNode(Node parent, ColumnNode child, int index) {
        TrimmingColumnNode replacement = new TrimmingColumnNode(child);
        replaceChild(parent, index, replacement);
    }

    @Override
    protected void onValueNode(Node parent, ValueNode child, int index) {
        Node replacement = new DerbyValueNode(child.getValue(), child.isArray(), child.getAttribute(), child.isNeedBinding());
        replaceChild(parent, index, replacement, false);
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        Node replacement = getReplacementForFunction(child);
        if(replacement != null) {
            replaceChild(parent, index, replacement);
        }
    }

    private Node getReplacementForFunction(FunctionNode child) {
        switch (child.getFunctionName()) {
            case "SUBSTRING":
                return new FunctionNode("SUBSTR", child.getAlias(), true);
            case "DAY_OF_MONTH":
                return new FunctionNode("DAY", child.getAlias(), true);
            case "DAY_OF_WEEK":
                return new FunctionNode("DAYOFWEEK", child.getAlias(), true);
            case "DAY_OF_YEAR":
                return new FunctionNode("DAYOFYEAR", child.getAlias(), true);
            case "CURRENT_DATE":
            case "CURRENT_TIME":
            case "CURRENT_TIMESTAMP":
                return new FunctionNode(child.getFunctionName(), child.getAlias(), false);
            case "CONCAT":
                return new OpExpressionNode("||");
        }
        return null;
    }

}

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

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.dba.sqlserver.sqltree.SQLServerLimitOffsetNode;

/**
 * SQL tree processor that supports OFFSET X ROWS FETCH NEXT Y ROWS ONLY clause
 * for the SQLServer 2012 and later.
 *
 * @since 4.2
 */
public class SQLServerTreeProcessorV12 extends SQLServerTreeProcessor {

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        if (hasOrderingClause(parent)) {
            replaceChild(parent, index, new SQLServerLimitOffsetNode(child.getLimit(), child.getOffset()), false);
            return;
        }
        super.onLimitOffsetNode(parent, child, index);
    }

    protected boolean hasOrderingClause(Node parent) {
        if(parent == null) {
            return false;
        }
        for (int i = 0; i < parent.getChildrenCount(); i++) {
            if (parent.getChild(i).getType() == NodeType.ORDER_BY) {
                return true;
            }
        }
        return hasOrderingClause(parent.getParent());
    }

}

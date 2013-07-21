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
package org.apache.cayenne.dba.openbase;

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.access.trans.JoinStack;
import org.apache.cayenne.access.trans.JoinTreeNode;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

/**
 * OpenBase does not support standard JOIN keyword and have strange syntax for defining
 * inner/outer joins
 * 
 * @see http
 *      ://www.openbase.com/help/KnowledgeBase/400_OpenBaseSQL/401_SelectStatements.html
 * @since 3.0
 */
class OpenBaseJoinStack extends JoinStack {

    protected OpenBaseJoinStack(DbAdapter dbAdapter, DataMap dataMap, QueryAssembler assembler) {
        super(dbAdapter, dataMap, assembler);
    }

    @Override
    protected void appendJoinSubtree(Appendable out, JoinTreeNode node)
            throws IOException {
        DbRelationship relationship = node.getRelationship();

        if (relationship == null) {
            return;
        }

        DbEntity targetEntity = (DbEntity) relationship.getTargetEntity();
        String targetAlias = node.getTargetTableAlias();

        out.append(", ").append(targetEntity.getFullyQualifiedName()).append(' ').append(
                targetAlias);

        for (JoinTreeNode child : node.getChildren()) {
            appendJoinSubtree(out, child);
        }
    }

    @Override
    protected void appendQualifier(Appendable out, boolean firstQualifierElement)
            throws IOException {
        boolean first = firstQualifierElement;
        for (JoinTreeNode node : rootNode.getChildren()) {
            if (!first) {
                out.append(" AND ");
            }
            appendQualifierSubtree(out, node);
            first = false;
        }
    }

    protected void appendQualifierSubtree(Appendable out, JoinTreeNode node)
            throws IOException {
        DbRelationship relationship = node.getRelationship();

        String srcAlias = node.getSourceTableAlias();
        String targetAlias = node.getTargetTableAlias();

        List<DbJoin> joins = relationship.getJoins();
        int len = joins.size();
        for (int i = 0; i < len; i++) {
            DbJoin join = joins.get(i);

            if (i > 0) {
                out.append(" AND ");
            }

            out.append(srcAlias).append('.').append(join.getSourceName());

            switch (node.getJoinType()) {
                case INNER:
                    out.append(" = ");
                    break;
                case LEFT_OUTER:
                    out.append(" * ");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported join type: "
                            + node.getJoinType());
            }

            out.append(targetAlias).append('.').append(join.getTargetName());

        }

        for (JoinTreeNode child : node.getChildren()) {
            out.append(" AND ");
            appendQualifierSubtree(out, child);
        }

    }

}

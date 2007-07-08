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

import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;

/** 
 * Uses Postgres extensions to optimize various translations. 
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
public class PostgresQualifierTranslator extends TrimmingQualifierTranslator {

    public PostgresQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler, "RTRIM");
    }

    public void startNode(Expression node, Expression parentNode) {

        if (node.getOperandCount() == 2) {
            // binary nodes are the only ones that currently require this
            detectObjectMatch(node);

            if (parenthesisNeeded(node, parentNode)) {
                qualBuf.append('(');
            }

            // super implementation has special handling 
            // of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
            // Postgres uses ILIKE
            // ...
        }
        else {
            super.startNode(node, parentNode);
        }
    }

    public void endNode(Expression node, Expression parentNode) {
        if (node.getOperandCount() == 2) {
            // check if we need to use objectMatchTranslator to finish building the expression
            if (matchingObject) {
                appendObjectMatch();
            }

            if (parenthesisNeeded(node, parentNode))
                qualBuf.append(')');

            // super implementation has special handling 
            // of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
            // Postgres uses ILIKE
            // ...
        }
        else {
            super.endNode(node, parentNode);
        }
    }

    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
        if (!hasMoreChildren) {
            return;
        }

        // use ILIKE

        switch (node.getType()) {

            case Expression.LIKE_IGNORE_CASE :
                finishedChildNodeAppendExpression(node, " ILIKE ");
                break;
            case Expression.NOT_LIKE_IGNORE_CASE :
                finishedChildNodeAppendExpression(node, " NOT ILIKE ");
                break;
            default :
                super.finishedChild(node, childIndex, hasMoreChildren);
        }
    }

    private void finishedChildNodeAppendExpression(Expression node, String operation) {
        StringBuffer buf = (matchingObject) ? new StringBuffer() : qualBuf;
        buf.append(operation);
        if (matchingObject) {
            objectMatchTranslator.setOperation(buf.toString());
            objectMatchTranslator.setExpression(node);
        }
    }
}

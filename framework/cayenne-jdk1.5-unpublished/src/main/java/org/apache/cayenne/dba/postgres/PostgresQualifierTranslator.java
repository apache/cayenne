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

import java.io.IOException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.PatternMatchNode;

/**
 * Uses Postgres extensions to optimize various translations.
 * 
 * @since 1.1
 */
public class PostgresQualifierTranslator extends TrimmingQualifierTranslator {

    public PostgresQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler, "RTRIM");
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {

        if (node.getOperandCount() == 2) {
            // binary nodes are the only ones that currently require this
            detectObjectMatch(node);

            try {
                if (parenthesisNeeded(node, parentNode)) {
                    out.append('(');
                }

                // super implementation has special handling
                // of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
                // Postgres uses ILIKE
                // ...
            }
            catch (IOException ioex) {
                throw new CayenneRuntimeException("Error appending content", ioex);
            }
        }
        else {
            super.startNode(node, parentNode);
        }
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        if (node.getOperandCount() == 2) {

            try {
                // check if we need to use objectMatchTranslator to finish building the
                // expression
                if (matchingObject) {
                    appendObjectMatch();
                }
                
                if(PatternMatchNode.class.isAssignableFrom(node.getClass()))
                    appendLikeEscapeCharacter((PatternMatchNode) node);

                if (parenthesisNeeded(node, parentNode))
                    out.append(')');

                // super implementation has special handling
                // of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
                // Postgres uses ILIKE
                // ...
            }
            catch (IOException ioex) {
                throw new CayenneRuntimeException("Error appending content", ioex);
            }
        }
        else {
            super.endNode(node, parentNode);
        }
    }

    @Override
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
        if (!hasMoreChildren) {
            return;
        }

        try {
            // use ILIKE

            switch (node.getType()) {

                case Expression.LIKE_IGNORE_CASE:
                    finishedChildNodeAppendExpression(node, " ILIKE ");
                    break;
                case Expression.NOT_LIKE_IGNORE_CASE:
                    finishedChildNodeAppendExpression(node, " NOT ILIKE ");
                    break;
                default:
                    super.finishedChild(node, childIndex, hasMoreChildren);
            }
        }
        catch (IOException ioex) {
            throw new CayenneRuntimeException("Error appending content", ioex);
        }
    }

    private void finishedChildNodeAppendExpression(Expression node, String operation)
            throws IOException {
        Appendable buf = (matchingObject) ? new StringBuilder() : this.out;
        buf.append(operation);
        if (matchingObject) {
            objectMatchTranslator.setOperation(buf.toString());
            objectMatchTranslator.setExpression(node);
        }
    }
}

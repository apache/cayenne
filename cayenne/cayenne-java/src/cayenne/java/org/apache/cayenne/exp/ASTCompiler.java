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

package org.apache.cayenne.exp;

import java.util.HashMap;
import java.util.Map;

/**
 * A compiler for Cayenne Expressions.
 * 
 * @since 1.0.6
 * @deprecated since 1.2
 * @author Andrus Adamchik
 */
class ASTCompiler {

    /**
     * Produces a chain of ASTNodes, returning the starting node that can be used
     * to evaluate expressions.
     */
    static ASTNode compile(Expression expression) throws ExpressionException {
        ExpressionParser handler = new ExpressionParser();
        expression.traverse(handler);
        return handler.finishParsing(expression);
    }

    static final class ExpressionParser extends TraversalHelper {
        // TODO - for big expressions we may remove cached AST from the map
        // once a node and all its children are procesed, to keep the map as small 
        // as possible during compilation... Though most data expressions are rather small,
        // and have from a few to a few dozen nodes...

        ASTNode currentNode;
        ASTNode startNode;
        Map astMap = new HashMap();

        ASTNode finishParsing(Expression expression) {
            // must add the top node to the tree. since "finishedChild"
            // is never invoked for the top node as child. 
            processNode((ASTNode) astMap.get(expression));

            return startNode;
        }

        public void startNode(Expression node, Expression parentNode) {
            // create an ASTNode and store it so that children could link 
            // to it.
            ASTNode parentAST = (ASTNode) astMap.get(parentNode);
            astMap.put(node, ASTNode.buildExpressionNode(node, parentAST));
        }

        public void finishedChild(
            Expression node,
            int childIndex,
            boolean hasMoreChildren) {

            // skip children of precompiled nodes, such as
            // OBJ_PATH, LIST, and varieties of LIKE
            int type = node.getType();
            if (type == Expression.OBJ_PATH
                || type == Expression.LIST
                || (childIndex == 1
                    && (type == Expression.LIKE
                        || type == Expression.LIKE_IGNORE_CASE
                        || type == Expression.NOT_LIKE
                        || type == Expression.NOT_LIKE_IGNORE_CASE))) {
                return;
            }

            // add expression to the chain
            Object child = node.getOperand(childIndex);

            // unless child is not an expression, it must be cached already
            ASTNode newAST;
            if (child instanceof Expression) {
                newAST = (ASTNode) astMap.get(child);
            }
            else {
                ASTNode parentAST = (ASTNode) astMap.get(node);
                newAST = ASTNode.buildObjectNode(child, parentAST);
            }

            processNode(newAST);
        }

        void processNode(ASTNode newAST) {
            if (startNode == null) {
                startNode = newAST;
                currentNode = newAST;
            }
            else {
                currentNode.setNextNode(newAST);
                currentNode = newAST;
            }
        }
    }
}

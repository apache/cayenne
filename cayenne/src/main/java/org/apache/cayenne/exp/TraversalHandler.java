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

package org.apache.cayenne.exp;

/**
 * Expression visitor interface. Defines callback methods invoked when 
 * walking the expression using {@link Expression#traverse(TraversalHandler)}.
 * 
 */
public interface TraversalHandler {

    /** 
     * Called during traversal after a child of expression
     * has been visited. 
     */
    default void finishedChild(
        Expression node,
        int childIndex,
        boolean hasMoreChildren) {
    }
  
    /** 
     * Called during the traversal before an expression node children
     * processing is started.
     * 
     * @since 1.1
     */
    default void startNode(Expression node, Expression parentNode) {
    }
    
    /** 
     * Called during the traversal after an expression node children
     * processing is finished.
     * 
     * @since 1.1
     */
    default void endNode(Expression node, Expression parentNode) {
    }
    
    /** 
     * Called during the traversal when a leaf non-expression node 
     * is encountered.
     */
    default void objectNode(Object leaf, Expression parentNode) {
    }
}

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
package org.apache.cayenne.ejbql;

/**
 * An abstract EJBQL expression interface.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public interface EJBQLExpression {

    /**
     * Accepts a visitor, calling appropriate visit method. If the visit method returns
     * true, visits all children, otherwise stops. Returns true if self visitor method and
     * all child methods returned true; false otherwise.
     */
    boolean visit(EJBQLExpressionVisitor visitor);

    /**
     * Returns a number of child operands of this expression node.
     */
    int getChildrenCount();

    /**
     * Returns a child expression node at the specified index.
     */
    EJBQLExpression getChild(int index);

    /**
     * Returns a text property of the node.
     */
    String getText();
}

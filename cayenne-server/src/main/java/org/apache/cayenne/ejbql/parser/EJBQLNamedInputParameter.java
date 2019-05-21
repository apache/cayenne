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
package org.apache.cayenne.ejbql.parser;

import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * @since 3.0
 */
public class EJBQLNamedInputParameter extends SimpleNode {

    public EJBQLNamedInputParameter(int id) {
        super(id);
    }

    @Override
    protected boolean visitNode(EJBQLExpressionVisitor visitor) {

        // this special handling caters for the fact that if a named parameter is used
        // in an IN clause and it is the only parameter and it is a collection of objects
        // then it should be bound as a number of objects rather than as that collection.

        if(null!=parent && EJBQLIn.class.isAssignableFrom(parent.getClass())) {
            EJBQLIn parentIn = (EJBQLIn) parent;

            // the count here is two; 0 is the expression to the thing that should IN the
            // list and 1... is the list itself.
            if(2==parentIn.getChildrenCount() && this==parentIn.getChild(1)) {
                return visitor.visitNamedInputParameterForIn(this);
            }
        }

        return visitor.visitNamedInputParameter(this);
    }
}

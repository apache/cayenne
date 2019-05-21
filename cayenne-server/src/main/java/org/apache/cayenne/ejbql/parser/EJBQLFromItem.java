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

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * @since 3.0
 */
public class EJBQLFromItem extends SimpleNode {

    public EJBQLFromItem(int id) {
        super(id);
    }

    /**
     * Returns an id generated from the entity name. It is used when no user-specified id
     * exists.
     */
    public String getSyntheticId() {
        int len = getChildrenCount();
        if (len < 1) {
            return null;
        }

        final String[] entityNames = new String[1];
        getChild(0).visit(new EJBQLBaseVisitor() {

            @Override
            public boolean visitIdentificationVariable(EJBQLExpression expression) {
                entityNames[0] = expression.getText();
                return false;
            }
        });
        
        if (entityNames[0] == null) {
            return null;
        }

        // id's are case insensitive, while entity names are. Using simple encoding to
        // transform the entity name in such way that two entities that differ only in
        // capitalization would produce different lowercase ids

        StringBuilder id = new StringBuilder(entityNames[0].length() + 2);
        for (int i = 0; i < entityNames[0].length(); i++) {
            char c = entityNames[0].charAt(i);
            if (Character.isUpperCase(c)) {
                id.append('%').append(Character.toLowerCase(c));
            }
            else {
                id.append(c);
            }
        }

        return id.toString();

    }

    public String getId() {
        int len = getChildrenCount();
        if (len < 2) {
            return getSyntheticId();
        }

        return jjtGetChild(len - 1).getText();
    }

    @Override
    protected boolean visitNode(EJBQLExpressionVisitor visitor) {
        return visitor.visitFromItem(this, -1);
    }

    @Override
    protected boolean visitChild(EJBQLExpressionVisitor visitor, int childIndex) {
        return super.visitChild(visitor, childIndex)
                && visitor.visitFromItem(this, childIndex);
    }
}

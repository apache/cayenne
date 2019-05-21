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
public class EJBQLPath extends SimpleNode {

    public EJBQLPath(int id) {
        super(id);
    }

    public String getId() {
        return (getChildrenCount() > 0) ? jjtGetChild(0).getText() : null;
    }

    public String getRelativePath() {
        int len = getChildrenCount();
        if (len < 2) {
            return null;
        }

        StringBuilder buffer = new StringBuilder(jjtGetChild(1).getText());
        for (int i = 2; i < len; i++) {
            buffer.append('.').append(jjtGetChild(i).getText());
        }

        return buffer.toString();
    }

    public String getAbsolutePath() {
        int len = getChildrenCount();
        if (len < 1) {
            return null;
        }

        StringBuilder buffer = new StringBuilder(jjtGetChild(0).getText());
        for (int i = 1; i < len; i++) {
            buffer.append('.').append(jjtGetChild(i).getText());
        }

        return buffer.toString();
    }

    @Override
    protected boolean visitNode(EJBQLExpressionVisitor visitor) {
        return visitor.visitPath(this, -1);
    }

    @Override
    protected boolean visitChild(EJBQLExpressionVisitor visitor, int childIndex) {
        children[childIndex].visit(visitor);
        return visitor.visitPath(this, childIndex);
    }
}

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

package org.apache.cayenne.exp.parser;

import java.util.Iterator;

import org.apache.cayenne.map.Entity;

/**
 * Generic path expression.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
public abstract class ASTPath extends SimpleNode {
    protected String path;

    ASTPath(int i) {
        super(i);
    }

    public int getOperandCount() {
        return 1;
    }

    public Object getOperand(int index) {
        if (index == 0) {
            return path;
        }

        throw new ArrayIndexOutOfBoundsException(index);
    }

    public void setOperand(int index, Object value) {
        if (index != 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        setPath(value);
    }

    protected void setPath(Object path) {
        this.path = (path != null) ? path.toString() : null;
    }

    protected String getPath() {
        return path;
    }
    

    /**
     * Helper method to evaluate path expression with Cayenne Entity.
     */
    protected Object evaluateEntityNode(Entity entity) {
        Iterator path = entity.resolvePathComponents(this);
        Object next = null;
        while (path.hasNext()) {
            next = path.next();
        }

        return next;
    }

    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException(
            "No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id] + "'");
    }
}

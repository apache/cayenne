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
import java.util.Map;

import org.apache.cayenne.map.Entity;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Generic path expression.
 * 
 * @since 1.1
 */
public abstract class ASTPath extends SimpleNode {

    protected String path;
    protected Map<String, String> pathAliases;

    ASTPath(int i) {
        super(i);
    }

    @Override
    public int getOperandCount() {
        return 1;
    }

    @Override
    public Object getOperand(int index) {
        if (index == 0) {
            return path;
        }

        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    public void setOperand(int index, Object value) {
        if (index != 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        setPath(value);
    }

    protected void setPath(Object path) {
        this.path = (path != null) ? path.toString() : null;
    }

    public String getPath() {
        return path;
    }

    /**
     * @since 3.0
     */
    @Override
    public Map<String, String> getPathAliases() {
        return pathAliases != null ? pathAliases : super.getPathAliases();
    }
    
    /**
     * @since 3.0
     */
    public void setPathAliases(Map<String, String> pathAliases) {
        this.pathAliases = pathAliases;
    }

    /**
     * Helper method to evaluate path expression with Cayenne Entity.
     */
    protected CayenneMapEntry evaluateEntityNode(Entity entity) {
        Iterator<CayenneMapEntry> path = entity.resolvePathComponents(this);
        CayenneMapEntry next = null;
        while (path.hasNext()) {
            next = path.next();
        }

        return next;
    }

    @Override
    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException("No operator for '"
                + ExpressionParserTreeConstants.jjtNodeName[id]
                + "'");
    }
}

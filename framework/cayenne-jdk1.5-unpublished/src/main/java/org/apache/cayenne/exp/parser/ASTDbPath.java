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

import java.io.IOException;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.Entity;

/**
 * Path expression traversing DB relationships and attributes.
 * 
 * @since 1.1
 */
public class ASTDbPath extends ASTPath {

    public static final String DB_PREFIX = "db:";

    ASTDbPath(int id) {
        super(id);
    }

    public ASTDbPath() {
        super(ExpressionParserTreeConstants.JJTDBPATH);
    }

    public ASTDbPath(Object value) {
        super(ExpressionParserTreeConstants.JJTDBPATH);
        setPath(value);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        // TODO: implement resolving DB_PATH for DataObjects

        if (o instanceof Entity) {
            return evaluateEntityNode((Entity) o);
        }

        Map<?, ?> map = toMap(o);
        return (map != null) ? map.get(path) : null;
    }

    protected Map<?, ?> toMap(Object o) {
        if (o instanceof Map) {
            return (Map<?, ?>) o;
        } else if (o instanceof ObjectId) {
            return ((ObjectId) o).getIdSnapshot();
        } else if (o instanceof Persistent) {
            Persistent persistent = (Persistent) o;

            // TODO: returns ObjectId snapshot for now.. should probably
            // retrieve full snapshot...
            ObjectId oid = persistent.getObjectId();
            return (oid != null) ? oid.getIdSnapshot() : null;
        } else {
            return null;
        }
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        ASTDbPath copy = new ASTDbPath(id);
        copy.path = path;
        return copy;
    }

    /**
     * @since 3.2
     */
    @Override
    public void appendAsEJBQL(Appendable out, String rootId) throws IOException {
        // warning: non-standard EJBQL...
        out.append(DB_PREFIX);
        out.append(rootId);
        out.append('.');
        out.append(path);
    }

    /**
     * @since 3.2
     */
    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append(DB_PREFIX).append(path);
    }

    @Override
    public int getType() {
        return Expression.DB_PATH;
    }
}

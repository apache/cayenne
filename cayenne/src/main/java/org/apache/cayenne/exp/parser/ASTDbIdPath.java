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

package org.apache.cayenne.exp.parser;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 4.2
 */
public class ASTDbIdPath extends ASTDbPath {

    public static final String DBID_PREFIX = "dbid:";

    ASTDbIdPath(int id) {
        super(id);
    }

    public ASTDbIdPath() {
        super(ExpressionParserTreeConstants.JJTDBIDPATH);
    }

    public ASTDbIdPath(String value) {
        super(ExpressionParserTreeConstants.JJTDBIDPATH);
        setPath(value);
    }

    public ASTDbIdPath(CayennePath value) {
        super(ExpressionParserTreeConstants.JJTDBIDPATH);
        setPath(value);
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        ASTDbIdPath copy = new ASTDbIdPath(id);
        copy.path = path;
        return copy;
    }

    protected Object evaluateNode(Object o, CayennePath localPath) {
        CayennePath objPath = localPath.parent();
        CayennePathSegment id = localPath.last();
        CayennePath nextSegment;

        if (localPath.length() > 1) {
            // nested entity
            if(o instanceof Persistent) {
                o = ((Persistent) o).readNestedProperty(objPath);
                nextSegment = CayennePath.of(List.of(id));
            } else {
                nextSegment = localPath;
            }
        } else {
            nextSegment = localPath;
        }

        if (o instanceof Persistent) {
            return toMap(o).get(id.value());
        } else if(o instanceof Collection) {
            return ((Collection<?>) o).stream()
                    .map(o1 -> evaluateNode(o1, nextSegment))
                    .collect(Collectors.toList());
        }

        return null;
    }

    @Override
    protected Object evaluateNode(Object o) {
        if (o instanceof Entity) {
            return evaluateEntityNode((Entity<?,?,?>) o);
        }
        return evaluateNode(o, path);
    }

    @Override
    protected CayenneMapEntry evaluateEntityNode(Entity<?,?,?> entity) {
        CayennePath objPath = path.parent();
        String id = path.last().value();

        if(!(entity instanceof ObjEntity)) {
            throw new CayenneRuntimeException("Unable to evaluate DBID path for DbEntity");
        }

        ObjEntity objEntity = (ObjEntity)entity;

        if(objPath != null) {
            CayenneMapEntry entry = new ASTObjPath(objPath).evaluateEntityNode(objEntity);
            if(!(entry instanceof ObjRelationship)) {
                throw new CayenneRuntimeException("Unable to evaluate DBID path %s, relationship expected", path);
            }
            objEntity = ((ObjRelationship) entry).getTargetEntity();
        }

        DbAttribute pk = objEntity.getDbEntity().getAttribute(id);
        if(pk == null || !pk.isPrimaryKey()) {
            throw new CayenneRuntimeException("Unable to find PK %s for entity %s", id, objEntity.getName());
        }
        return pk;
    }

    @Override
    protected Map<?, ?> toMap(Object o) {
        if (o instanceof Map) {
            return (Map<?, ?>) o;
        } else if (o instanceof ObjectId) {
            return ((ObjectId) o).getIdSnapshot();
        } else if (o instanceof Persistent) {
            return ((Persistent) o).getObjectId().getIdSnapshot();
        } else {
            return null;
        }
    }

    @Override
    public int getType() {
        return Expression.DBID_PATH;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        // NOTE: append as db path
        out.append(DB_PREFIX);
        out.append(rootId);
        out.append('.');
        out.append(path.value());
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append(DBID_PREFIX).append(path.value());
    }
}

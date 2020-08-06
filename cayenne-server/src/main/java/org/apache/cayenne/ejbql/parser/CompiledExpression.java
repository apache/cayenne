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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A compiled EJBQL expression.
 * 
 * @since 3.0
 */
class CompiledExpression implements EJBQLCompiledExpression {

    private String source;
    private String rootId;
    private Map<String, ClassDescriptor> descriptorsById;
    private Map<String, ObjRelationship> incomingById;
    private EJBQLExpression expression;
    private SQLResult result;
    private PrefetchTreeNode prefetchTree;
    
    
    public ClassDescriptor getEntityDescriptor(String idVariable) {
        if (idVariable == null) {
            return null;
        }

        return descriptorsById.get(Compiler.normalizeIdPath(idVariable));
    }

    public SQLResult getResult() {
        return result;
    }

    public ClassDescriptor getRootDescriptor() {
        return rootId != null ? getEntityDescriptor(rootId) : null;
    }

    public List<DbRelationship> getIncomingRelationships(String identifier) {
        ObjRelationship relationship = incomingById.get(identifier);
        if (relationship == null) {
            return Collections.emptyList();
        }

        return relationship.getDbRelationships();
    }

    public EJBQLExpression getExpression() {
        return expression;
    }

    public String getSource() {
        return source;
    }

    void setExpression(EJBQLExpression expression) {
        this.expression = expression;
    }

    void setDescriptorsById(Map<String, ClassDescriptor> descriptorsById) {
        this.descriptorsById = descriptorsById;
    }

    void setIncomingById(Map<String, ObjRelationship> incomingById) {
        this.incomingById = incomingById;
    }

    void setSource(String source) {
        this.source = source;
    }

    void setRootId(String rootId) {
        this.rootId = rootId;
    }

    void setResult(SQLResult resultSetMapping) {
        this.result = resultSetMapping;
    }
    
    public PrefetchTreeNode getPrefetchTree() {
        return prefetchTree;
    }

    public void setPrefetchTree(PrefetchTreeNode prefetchTree) {
        this.prefetchTree = prefetchTree;
    }
}

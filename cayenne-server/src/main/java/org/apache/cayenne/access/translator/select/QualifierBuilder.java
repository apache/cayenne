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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.TraversalHelper;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.util.HashSet;
import java.util.Set;

public class QualifierBuilder extends TraversalHelper {

    protected QueryAssembler queryAssembler;
    protected Set<Expression> addedQualifiers;
    protected ASTObjPath relPath;
    protected ObjEntity objEntity;
    protected Expression qualifier;

    public QualifierBuilder(Expression qualifier, ObjEntity objEntity, QueryAssembler queryAssembler) {
        this.qualifier = qualifier;
        this.objEntity = objEntity;
        this.queryAssembler = queryAssembler;
        this.relPath = new ASTObjPath();
        this.addedQualifiers = new HashSet<Expression>();
    }

    @Override
    public void endNode(Expression objPath, Expression parentNode) {
        if (objPath.getType() == Expression.DB_PATH) {
            qualifierForEntityAndSubclasses(objEntity);
        }

        if (objPath.getType() != Expression.OBJ_PATH) {
            return;
        }

        this.relPath = new ASTObjPath();

        Iterable<PathComponent<ObjAttribute, ObjRelationship>> pathComponents = objEntity.resolvePath(objPath, queryAssembler.getPathAliases());
        for (PathComponent<ObjAttribute, ObjRelationship> pathComponent : pathComponents) {
            if (pathComponent.isAlias()) {
                for (PathComponent<ObjAttribute, ObjRelationship> aliasedPathComponent : pathComponent.getAliasedPath()) {
                    if (extractQualifier(aliasedPathComponent)) {
                        StringBuilder ex = new StringBuilder();
                        ex
                                .append("Can't extract qualifiers for aliased path. Aliased path shouldn't contain any qualifiers")
                                .append(": [")
                                .append(pathComponent.getName())
                                .append(" -> ")
                                .append(relPath)
                                .append("].");
                        throw new ExpressionException(ex.toString());
                    };
                }
                continue;
            }

            extractQualifier(pathComponent);
        }
    }

    private boolean extractQualifier(PathComponent<ObjAttribute, ObjRelationship> pathComponent) {
        ObjAttribute attribute = pathComponent.getAttribute();
        ObjRelationship relationship = pathComponent.getRelationship();
        ObjEntity entity = (attribute != null) ? attribute.getEntity() : relationship.getSourceEntity();

        boolean extracted = qualifierForEntityAndSubclasses(entity);

        if (relationship != null) {
            relPath.appendPath(relationship.getName());
        }

        return extracted;
    }

    protected boolean qualifierForEntityAndSubclasses(ObjEntity entity) {
        ClassDescriptor descriptor = queryAssembler
                .getEntityResolver()
                .getClassDescriptor(entity.getName());

        Expression entityQualifier = descriptor
                .getEntityInheritanceTree()
                .qualifierForEntityAndSubclasses();

        if (entityQualifier != null) {
            if (!addedQualifiers.add(entityQualifier)) {
                return false;
            }

            QualifierBuilderHelper qualifierBuilder = new QualifierBuilderHelper(qualifier, entity, queryAssembler, relPath, addedQualifiers, entityQualifier);
            entityQualifier.traverse(qualifierBuilder);

            qualifier = qualifierBuilder.getQualifier();
            qualifier = (qualifier != null)
                    ? qualifier.andExp(qualifierBuilder.getAttachableQualifier())
                    : qualifierBuilder.getAttachableQualifier();

            return true;
        }

        return false;
    }

    public Expression getQualifier() {
        return qualifier;
    }

}

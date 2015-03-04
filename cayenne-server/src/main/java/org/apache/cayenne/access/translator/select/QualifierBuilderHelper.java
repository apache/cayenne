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
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;

public class QualifierBuilderHelper extends QualifierBuilder {

    private Expression parentQualifier;
    private Expression attachableQualifier;
    private ASTObjPath buffRelPath;

    public QualifierBuilderHelper(Expression qualifier, ObjEntity objEntity, QueryAssembler queryAssembler, ASTObjPath relPath, Expression parentQualifier) {
        super(qualifier, objEntity, queryAssembler);
        this.relPath = relPath;
        this.buffRelPath = (ASTObjPath) relPath.shallowCopy();
        this.parentQualifier = parentQualifier;
    }

    @Override
    public void endNode(Expression objPath, Expression parentNode) {
        if (objPath.getType() == Expression.DB_PATH) {
            qualifierForEntityAndSubclasses(objEntity);
        }

        if (objPath.getType() != Expression.OBJ_PATH) {
            return;
        }

        Iterable<PathComponent<ObjAttribute, ObjRelationship>> pathComponents = objEntity.resolvePath(objPath, queryAssembler.getPathAliases());
        for (PathComponent<ObjAttribute, ObjRelationship> pathComponent : pathComponents) {
            ObjAttribute attribute = pathComponent.getAttribute();
            if (attribute != null) {
                Expression entityQualifier = relPath.prependToExpression(parentNode);
                switch (parentQualifier.getType()) {
                    case Expression.AND:
                        attachableQualifier = (attachableQualifier != null) ? attachableQualifier.andExp(entityQualifier) : entityQualifier;
                        break;
                    case Expression.OR:
                        attachableQualifier = (attachableQualifier != null) ? attachableQualifier.orExp(entityQualifier) : entityQualifier;
                        break;
                    default:
                        attachableQualifier = entityQualifier;
                }

                break;
            }

            ObjRelationship relationship = pathComponent.getRelationship();

            relPath.appendPath(relationship.getName());
            qualifierForEntityAndSubclasses(relationship.getTargetEntity());
            relPath = buffRelPath;
        }
    }

    public Expression getAttachableQualifier() {
        return attachableQualifier;
    }

}

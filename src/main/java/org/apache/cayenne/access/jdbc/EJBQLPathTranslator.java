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
package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLPath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

class EJBQLPathTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private ObjEntity currentEntity;
    private String lastPathComponent;
    private String idPath;

    EJBQLPathTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
    }

    public boolean visitPath(EJBQLPath expression, int finishedChildIndex) {

        if (finishedChildIndex > 0) {

            if (finishedChildIndex + 1 < expression.getChildrenCount()) {
                processIntermediatePath();
            }
            else {
                processLastPath();
            }
        }
        return true;
    }

    public boolean visitIdentifier(EJBQLExpression expression) {
        ClassDescriptor descriptor = context.getCompiledExpression().getEntityDescriptor(
                expression.getText());
        if (descriptor == null) {
            throw new EJBQLException("Invalid identification variable: "
                    + expression.getText());
        }

        this.currentEntity = descriptor.getEntity();
        this.idPath = expression.getText();
        return true;
    }

    public boolean visitIdentificationVariable(EJBQLExpression expression) {
        if (this.lastPathComponent != null) {
            this.idPath += '.' + lastPathComponent;
        }

        this.lastPathComponent = expression.getText();
        return true;
    }

    private void processIntermediatePath() {
        ObjRelationship relationship = (ObjRelationship) currentEntity
                .getRelationship(lastPathComponent);
        if (relationship == null) {
            throw new EJBQLException("Unknown relationship '"
                    + lastPathComponent
                    + "' for entity '"
                    + currentEntity.getName()
                    + "'");
        }

        this.currentEntity = (ObjEntity) relationship.getTargetEntity();
    }

    private void processLastPath() {
        // TODO: andrus 3/25/2007 - process terminal relationships
        ObjAttribute attribute = (ObjAttribute) currentEntity
                .getAttribute(lastPathComponent);

        DbEntity table = currentEntity.getDbEntity();
        String alias = context.createAlias(idPath, table.getFullyQualifiedName());
        context.append(' ').append(alias).append('.').append(
                attribute.getDbAttributeName());
    }
}

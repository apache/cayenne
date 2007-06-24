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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLIdentificationVariable;
import org.apache.cayenne.ejbql.parser.EJBQLIdentifier;
import org.apache.cayenne.ejbql.parser.EJBQLInnerJoin;
import org.apache.cayenne.ejbql.parser.EJBQLPath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A translator that walks the relationship/attribute path, appending joins to the query.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
abstract class EJBQLPathTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    protected ObjEntity currentEntity;
    private String lastPathComponent;
    protected String lastAlias;
    protected String idPath;
    private String fullPath;
    private EJBQLFromTranslator joinAppender;

    EJBQLPathTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
    }

    protected abstract void appendMultiColumnPath(EJBQLMultiColumnOperand operand);

    public boolean visitPath(EJBQLPath expression, int finishedChildIndex) {

        if (finishedChildIndex > 0) {

            if (finishedChildIndex + 1 < expression.getChildrenCount()) {
                processIntermediatePathComponent();
            }
            else {
                processLastPathComponent();
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
        this.fullPath = idPath;
        return true;
    }

    public boolean visitIdentificationVariable(EJBQLExpression expression) {

        // TODO: andrus 6/11/2007 - if the path ends with relationship, the last join will
        // get lost...
        if (lastPathComponent != null) {
            resolveJoin();
        }

        this.lastPathComponent = expression.getText();
        return true;
    }

    private EJBQLFromTranslator getJoinAppender() {
        if (joinAppender == null) {
            joinAppender = new EJBQLFromTranslator(context);
        }

        return joinAppender;
    }

    private void resolveJoin() {

        String newPath = idPath + '.' + lastPathComponent;
        String oldPath = context.registerReusableJoin(idPath, lastPathComponent, newPath);

        this.fullPath = fullPath + '.' + lastPathComponent;

        if (oldPath != null) {
            this.idPath = oldPath;
            this.lastAlias = context.getTableAlias(oldPath, currentEntity
                    .getDbEntityName());
        }
        else {
            // register join
            EJBQLIdentifier id = new EJBQLIdentifier(-1);
            id.setText(idPath);

            EJBQLIdentificationVariable idVar = new EJBQLIdentificationVariable(-1);
            idVar.setText(lastPathComponent);

            EJBQLPath path = new EJBQLPath(-1);
            path.jjtAddChild(id, 0);
            path.jjtAddChild(idVar, 1);

            EJBQLIdentifier joinId = new EJBQLIdentifier(-1);
            joinId.setText(fullPath);

            EJBQLInnerJoin join = new EJBQLInnerJoin(-1);
            join.jjtAddChild(path, 0);
            join.jjtAddChild(joinId, 1);

            context.switchToMarker(EJBQLTranslationContext.FROM_TAIL_MARKER);

            getJoinAppender().visitInnerJoin(join);
            context.switchToMainBuffer();

            this.idPath = newPath;
            this.lastAlias = context.getTableAlias(fullPath, currentEntity
                    .getDbEntityName());
        }
    }

    private void processIntermediatePathComponent() {
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

    private void processLastPathComponent() {

        ObjAttribute attribute = (ObjAttribute) currentEntity
                .getAttribute(lastPathComponent);

        if (attribute != null) {
            processTerminatingAttribute(attribute);
            return;
        }

        ObjRelationship relationship = (ObjRelationship) currentEntity
                .getRelationship(lastPathComponent);
        if (relationship != null) {
            processTerminatingRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Invalid path component: " + lastPathComponent);
    }

    protected void processTerminatingAttribute(ObjAttribute attribute) {

        DbEntity table = currentEntity.getDbEntity();
        String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(
                idPath,
                table.getFullyQualifiedName());
        context.append(' ').append(alias).append('.').append(
                attribute.getDbAttributeName());
    }

    private void processTerminatingRelationship(ObjRelationship relationship) {

        // check whether we need a join
        if (relationship.isSourceIndependentFromTargetChange()) {
            // TODO: andrus, 6/13/2007 - implement
        }
        else {
            // match FK against the target object

            // TODO: andrus, 6/21/2007 - flattened support
            DbRelationship dbRelationship = (DbRelationship) relationship
                    .getDbRelationships()
                    .get(0);
            DbEntity table = (DbEntity) dbRelationship.getSourceEntity();

            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(
                    idPath,
                    table.getFullyQualifiedName());

            List joins = dbRelationship.getJoins();

            if (joins.size() == 1) {
                DbJoin join = (DbJoin) joins.get(0);
                context
                        .append(' ')
                        .append(alias)
                        .append('.')
                        .append(join.getSourceName());
            }
            else {
                Map multiColumnMatch = new HashMap(joins.size() + 2);

                Iterator it = joins.iterator();
                while (it.hasNext()) {
                    DbJoin join = (DbJoin) it.next();
                    String column = alias + "." + join.getSourceName();

                    multiColumnMatch.put(join.getTargetName(), column);
                }

                appendMultiColumnPath(EJBQLMultiColumnOperand.getPathOperand(
                        context,
                        multiColumnMatch));
            }
        }
    }
}

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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A translator that walks the relationship/attribute path, appending joins to the query.
 * 
 * @since 3.0
 */
public abstract class EJBQLPathTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    protected ObjEntity currentEntity;
    protected String lastPathComponent;
    protected String lastAlias;
    protected String idPath;
    protected String joinMarker;
    protected String fullPath;
    private boolean usingAliases;

    public EJBQLPathTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
        this.usingAliases = true;
    }

    protected abstract void appendMultiColumnPath(EJBQLMultiColumnOperand operand);

    @Override
    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

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

    @Override
    public boolean visitIdentifier(EJBQLExpression expression) {
        ClassDescriptor descriptor = context.getEntityDescriptor(expression.getText());
        if (descriptor == null) {
            throw new EJBQLException("Invalid identification variable: "
                    + expression.getText());
        }

        this.currentEntity = descriptor.getEntity();
        this.idPath = expression.getText();
        this.joinMarker = EJBQLJoinAppender.makeJoinTailMarker(idPath);
        this.fullPath = idPath;
        return true;
    }

    @Override
    public boolean visitIdentificationVariable(EJBQLExpression expression) {

        // TODO: andrus 6/11/2007 - if the path ends with relationship, the last join will
        // get lost...
        if (lastPathComponent != null) {
            resolveJoin(true);
        }

        this.lastPathComponent = expression.getText();
        return true;
    }

    protected void resolveJoin(boolean inner) {

        EJBQLJoinAppender joinAppender = context.getTranslatorFactory().getJoinAppender(
                context);

        String newPath = idPath + '.' + lastPathComponent;
        String oldPath = joinAppender.registerReusableJoin(
                idPath,
                lastPathComponent,
                newPath);

        this.fullPath = fullPath + '.' + lastPathComponent;

        if (oldPath != null) {
            this.idPath = oldPath;
            Relationship lastRelationship = currentEntity
                    .getRelationship(lastPathComponent);
            if (lastRelationship != null) {
                ObjEntity targetEntity = (ObjEntity) lastRelationship.getTargetEntity();

                this.lastAlias = context.getTableAlias(fullPath, targetEntity
                        .getDbEntity()
                        .getFullyQualifiedName());
            }
            else {
                this.lastAlias = context.getTableAlias(oldPath, currentEntity
                        .getDbEntity()
                        .getFullyQualifiedName());

            }
        }
        else {
            Relationship lastRelationship = currentEntity
                    .getRelationship(lastPathComponent);

            ObjEntity targetEntity = null;
            if (lastRelationship != null) {
                targetEntity = (ObjEntity) lastRelationship.getTargetEntity();
            }
            else {
                targetEntity = currentEntity;
            }

            // register join
            if (inner) {
                joinAppender.appendInnerJoin(
                        joinMarker,
                        new EJBQLTableId(idPath),
                        new EJBQLTableId(fullPath));
            }
            else {
                joinAppender.appendOuterJoin(
                        joinMarker,
                        new EJBQLTableId(idPath),
                        new EJBQLTableId(fullPath));

            }

            this.lastAlias = context.getTableAlias(fullPath, targetEntity
                    .getDbEntity()
                    .getFullyQualifiedName());

            this.idPath = newPath;
        }
    }

    protected void processIntermediatePathComponent() {
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

    protected void processLastPathComponent() {

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
        
        DbEntity table = null;
        Iterator<?> it = attribute.getDbPathIterator();
        while (it.hasNext()) {
            Object pathComponent = it.next();
            if (pathComponent instanceof DbAttribute) {
                table = (DbEntity) ((DbAttribute) pathComponent).getEntity();
            }
        }

        if (isUsingAliases()) {
            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(
                    idPath,
                    table.getFullyQualifiedName());
            context.append(' ').append(alias).append('.').append(
                    attribute.getDbAttributeName());
        }
        else {
            context.append(' ').append(attribute.getDbAttributeName());
        }
    }

    protected void processTerminatingRelationship(ObjRelationship relationship) {

        if (relationship.isSourceIndependentFromTargetChange()) {

            // use an outer join for to-many matches
            resolveJoin(false);

            DbRelationship dbRelationship = chooseDbRelationship(relationship);
            DbEntity table = (DbEntity) dbRelationship.getTargetEntity();

            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(
                    idPath,
                    table.getFullyQualifiedName());
           
            
            Collection<DbAttribute> pks = table.getPrimaryKeys();

            if (pks.size() == 1) {
                DbAttribute pk = pks.iterator().next();
                context.append(' ');
                if (isUsingAliases()) {
                    context.append(alias).append('.');
                }
                context.append(pk.getName());
            }
            else {
                throw new EJBQLException(
                        "Multi-column PK to-many matches are not yet supported.");
            }
            
        }
        else {
            // match FK against the target object

            DbRelationship dbRelationship = chooseDbRelationship(relationship);
            DbEntity table = (DbEntity) dbRelationship.getSourceEntity();

            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(
                    idPath,
                    table.getFullyQualifiedName());

            List<DbJoin> joins = dbRelationship.getJoins();

            if (joins.size() == 1) {
                DbJoin join = joins.get(0);
                context.append(' ');
                if (isUsingAliases()) {
                    context.append(alias).append('.');
                }
                context.append(join.getSourceName());
            }
            else {
                Map<String, String> multiColumnMatch = new HashMap<String, String>(joins
                        .size() + 2);

                for (DbJoin join : joins) {
                    String column = isUsingAliases()
                            ? alias + "." + join.getSourceName()
                            : join.getSourceName();

                    multiColumnMatch.put(join.getTargetName(), column);
                }

                appendMultiColumnPath(EJBQLMultiColumnOperand.getPathOperand(
                        context,
                        multiColumnMatch));
            }
        }
    }
    
    /**
     * Checks if the object relationship is flattened and then chooses
     * the corresponding db relationship. The last in idPath if isFlattened and
     * the first in list otherwise.
     * 
     * @param relationship the object relationship
     * 
     * @return {@link DbRelationship}
     */
    protected DbRelationship chooseDbRelationship(ObjRelationship relationship) {

        List<DbRelationship> dbRelationships = relationship.getDbRelationships();
        String dbRelationshipPath = relationship.getDbRelationshipPath();

        if (dbRelationshipPath.contains(".")) {
            String dbRelName = dbRelationshipPath.substring(dbRelationshipPath
                    .lastIndexOf(".") + 1);
            for (DbRelationship dbR : dbRelationships) {
                if (dbR.getName().equals(dbRelName)) {
                    return dbR;
                }

            }
        }
        return relationship.getDbRelationships().get(0);

    }

    public boolean isUsingAliases() {
        return usingAliases;
    }

    public void setUsingAliases(boolean usingAliases) {
        this.usingAliases = usingAliases;
    }
}

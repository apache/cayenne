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
package org.apache.cayenne.access.translator.ejbql;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.reflect.ClassDescriptor;

public abstract class EJBQLDbPathTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    protected DbEntity currentEntity;
    private String lastPathComponent;
    protected String lastAlias;
    protected String idPath;
    protected String joinMarker;
    private String fullPath;
    private boolean usingAliases;

    public EJBQLDbPathTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
        this.usingAliases = true;
    }

    protected abstract void appendMultiColumnPath(EJBQLMultiColumnOperand operand);

    @Override
    public boolean visitDbPath(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex > 0) {

            if (finishedChildIndex + 1 < expression.getChildrenCount()) {
                processIntermediatePathComponent();
            } else {
                processLastPathComponent();
            }
        }

        return true;
    }

    @Override
    public boolean visitIdentifier(EJBQLExpression expression) {

        // expression id is always rooted in an ObjEntity, even for DbPath...
        ClassDescriptor descriptor = context.getEntityDescriptor(expression.getText());
        if (descriptor == null) {
            throw new EJBQLException("Invalid identification variable: " + expression.getText());
        }

        this.currentEntity = descriptor.getEntity().getDbEntity();
        this.idPath = expression.getText();
        this.joinMarker = EJBQLJoinAppender.makeJoinTailMarker(idPath);
        this.fullPath = idPath;
        return true;
    }

    @Override
    public boolean visitIdentificationVariable(EJBQLExpression expression) {

        // TODO: andrus 6/11/2007 - if the path ends with relationship, the last
        // join will
        // get lost...
        if (lastPathComponent != null) {
            resolveJoin(true);
        }

        this.lastPathComponent = expression.getText();
        return true;
    }

    private void resolveJoin(boolean inner) {

        EJBQLJoinAppender joinAppender = context.getTranslatorFactory().getJoinAppender(context);

        // TODO: andrus 1/6/2007 - conflict with object path naming... maybe
        // 'registerReusableJoin' should normalize everything to a db path?
        String newPath = idPath + '.' + lastPathComponent;
        String oldPath = joinAppender.registerReusableJoin(idPath, lastPathComponent, newPath);

        this.fullPath = fullPath + '.' + lastPathComponent;

        if (oldPath != null) {
            this.idPath = oldPath;
            this.lastAlias = context.getTableAlias(oldPath,
                    context.getQuotingStrategy().quotedFullyQualifiedName(currentEntity));
        } else {

            // register join
            if (inner) {
                joinAppender.appendInnerJoin(joinMarker, new EJBQLTableId(idPath), new EJBQLTableId(fullPath));
                this.lastAlias = context.getTableAlias(fullPath,
                        context.getQuotingStrategy().quotedFullyQualifiedName(currentEntity));
            } else {
                joinAppender.appendOuterJoin(joinMarker, new EJBQLTableId(idPath), new EJBQLTableId(fullPath));

                Relationship lastRelationship = currentEntity.getRelationship(lastPathComponent);
                DbEntity targetEntity = (DbEntity) lastRelationship.getTargetEntity();

                this.lastAlias = context.getTableAlias(fullPath,
                        context.getQuotingStrategy().quotedFullyQualifiedName(targetEntity));
            }

            this.idPath = newPath;
        }
    }

    private void processIntermediatePathComponent() {
        DbRelationship relationship = currentEntity.getRelationship(lastPathComponent);
        if (relationship == null) {
            throw new EJBQLException("Unknown relationship '" + lastPathComponent + "' for entity '"
                    + currentEntity.getName() + "'");
        }

        this.currentEntity = (DbEntity) relationship.getTargetEntity();
    }

    private void processLastPathComponent() {

        DbAttribute attribute = currentEntity.getAttribute(lastPathComponent);

        if (attribute != null) {
            processTerminatingAttribute(attribute);
            return;
        }

        DbRelationship relationship = currentEntity.getRelationship(lastPathComponent);
        if (relationship != null) {
            processTerminatingRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Invalid path component: " + lastPathComponent);
    }

    protected void processTerminatingAttribute(DbAttribute attribute) {

        DbEntity table = (DbEntity) attribute.getEntity();

        if (isUsingAliases()) {
            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(idPath, context
                    .getQuotingStrategy().quotedFullyQualifiedName(table));
            context.append(' ').append(alias).append('.').append(context.getQuotingStrategy().quotedName(attribute));
        } else {
            context.append(' ').append(context.getQuotingStrategy().quotedName(attribute));
        }
    }

    private void processTerminatingRelationship(DbRelationship relationship) {

        if (relationship.isToMany()) {

            // use an outer join for to-many matches
            resolveJoin(false);

            DbEntity table = (DbEntity) relationship.getTargetEntity();

            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(idPath, context
                    .getQuotingStrategy().quotedFullyQualifiedName(table));

            Collection<DbAttribute> pks = table.getPrimaryKeys();

            if (pks.size() == 1) {
                DbAttribute pk = pks.iterator().next();
                context.append(' ');
                if (isUsingAliases()) {
                    context.append(alias).append('.');
                }
                context.append(context.getQuotingStrategy().quotedName(pk));
            } else {
                throw new EJBQLException("Multi-column PK to-many matches are not yet supported.");
            }
        } else {
            // match FK against the target object

            DbEntity table = (DbEntity) relationship.getSourceEntity();

            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(idPath, context
                    .getQuotingStrategy().quotedFullyQualifiedName(table));

            List<DbJoin> joins = relationship.getJoins();

            if (joins.size() == 1) {
                DbJoin join = joins.get(0);
                context.append(' ');
                if (isUsingAliases()) {
                    context.append(alias).append('.');
                }
                context.append(context.getQuotingStrategy().quotedName(join.getSource()));
            } else {
                Map<String, String> multiColumnMatch = new HashMap<String, String>(joins.size() + 2);

                for (DbJoin join : joins) {
                    String column = isUsingAliases() ? alias + "." + join.getSourceName() : join.getSourceName();

                    multiColumnMatch.put(join.getTargetName(), column);
                }

                appendMultiColumnPath(EJBQLMultiColumnOperand.getPathOperand(context, multiColumnMatch));
            }
        }
    }

    public boolean isUsingAliases() {
        return usingAliases;
    }

    public void setUsingAliases(boolean usingAliases) {
        this.usingAliases = usingAliases;
    }
}

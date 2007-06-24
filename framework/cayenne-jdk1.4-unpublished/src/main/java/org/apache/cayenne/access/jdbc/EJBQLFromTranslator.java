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

import java.util.Iterator;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLFromItem;
import org.apache.cayenne.ejbql.parser.EJBQLJoin;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLFromTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private int fromCount;

    public EJBQLFromTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {

        if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.markCurrentPosition(EJBQLTranslationContext.FROM_TAIL_MARKER);
        }

        return true;
    }

    public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            if (fromCount++ > 0) {
                context.append(',');
            }
            appendTable(expression.getId());
        }

        return true;
    }

    public boolean visitInnerFetchJoin(EJBQLJoin join, int finishedChildIndex) {
        // TODO: andrus, 4/9/2007 - support for prefetching
        return visitInnerJoin(join, finishedChildIndex);
    }

    public boolean visitInnerJoin(EJBQLJoin join, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            appendJoin(join, "INNER JOIN", true);
        }
        return true;
    }

    public boolean visitOuterFetchJoin(EJBQLJoin join, int finishedChildIndex) {
        // TODO: andrus, 4/9/2007 - support for prefetching
        return visitOuterJoin(join, finishedChildIndex);
    }

    public boolean visitOuterJoin(EJBQLJoin join, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            appendJoin(join, "LEFT OUTER JOIN", false);
        }
        return true;
    }

    private void appendJoin(EJBQLJoin join, String semantics, boolean reusable) {

        String rhsId = join.getRightHandSideId();

        ObjRelationship joinRelationship = context
                .getCompiledExpression()
                .getIncomingRelationship(rhsId);
        if (joinRelationship == null) {
            throw new EJBQLException("No join configured for id " + rhsId);
        }

        // TODO: andrus, 4/8/2007 - support for flattened relationships
        DbRelationship incomingDB = (DbRelationship) joinRelationship
                .getDbRelationships()
                .get(0);

        String lhsId = join.getLeftHandSideId();
        String sourceAlias = context.getTableAlias(lhsId, incomingDB
                .getSourceEntity()
                .getName());

        context.append(" ").append(semantics);
        String targetAlias = appendTable(rhsId);
        context.append(" ON (");

        Iterator it = incomingDB.getJoins().iterator();
        if (it.hasNext()) {
            DbJoin dbJoin = (DbJoin) it.next();
            context
                    .append(sourceAlias)
                    .append('.')
                    .append(dbJoin.getSourceName())
                    .append(" = ")
                    .append(targetAlias)
                    .append('.')
                    .append(dbJoin.getTargetName());
        }

        while (it.hasNext()) {
            context.append(", ");
            DbJoin dbJoin = (DbJoin) it.next();
            context
                    .append(sourceAlias)
                    .append('.')
                    .append(dbJoin.getSourceName())
                    .append(" = ")
                    .append(targetAlias)
                    .append('.')
                    .append(dbJoin.getTargetName());
        }

        context.append(")");

        if (reusable) {
            context.registerReusableJoin(lhsId, joinRelationship.getName(), rhsId);
        }
    }

    private String appendTable(String id) {
        ClassDescriptor descriptor = context.getCompiledExpression().getEntityDescriptor(
                id);

        String tableName = descriptor.getEntity().getDbEntity().getFullyQualifiedName();
        String alias = context.getTableAlias(id, tableName);
        context.append(' ').append(tableName).append(" AS ").append(alias);
        return alias;
    }
}

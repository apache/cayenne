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
public class EJBQLFromTranslator extends EJBQLBaseVisitor {

    protected EJBQLTranslationContext context;
    private String lastId;

    static String makeJoinTailMarker(String id) {
        return "FROM_TAIL" + id;
    }

    public EJBQLFromTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            if (lastId != null) {
                context.markCurrentPosition(makeJoinTailMarker(lastId));
            }
        }

        return true;
    }

    public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {

        String id = expression.getId();

        if (lastId != null) {
            context.append(',');
            context.markCurrentPosition(makeJoinTailMarker(lastId));
        }

        this.lastId = id;
        appendTable(id);
        return false;
    }

    public boolean visitInnerFetchJoin(EJBQLJoin join) {
        // TODO: andrus, 4/9/2007 - support for prefetching
        return visitInnerJoin(join);
    }

    public boolean visitInnerJoin(EJBQLJoin join) {
        appendJoin(join, "INNER JOIN");
        return false;
    }

    public boolean visitOuterFetchJoin(EJBQLJoin join) {
        // TODO: andrus, 4/9/2007 - support for prefetching
        return visitOuterJoin(join);
    }

    public boolean visitOuterJoin(EJBQLJoin join) {
        appendJoin(join, "LEFT OUTER JOIN");
        return false;
    }

    private void appendJoin(EJBQLJoin join, String semantics) {

        String rhsId = join.getRightHandSideId();

        ObjRelationship joinRelationship = context.getIncomingRelationship(rhsId);
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
    }

    protected String appendTable(String id) {
        ClassDescriptor descriptor = context.getEntityDescriptor(id);

        String tableName = descriptor.getEntity().getDbEntity().getFullyQualifiedName();
        String alias = context.getTableAlias(id, tableName);

        // not using "AS" to separate table name and alias name - OpenBase doesn't support
        // "AS", and the rest of the databases do not care
        context.append(' ').append(tableName).append(' ').append(alias);
        return alias;
    }
}

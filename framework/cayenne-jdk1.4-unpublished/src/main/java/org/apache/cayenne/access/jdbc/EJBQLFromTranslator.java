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
import org.apache.cayenne.ejbql.parser.EJBQLFromItem;
import org.apache.cayenne.ejbql.parser.EJBQLJoin;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

public class EJBQLFromTranslator extends EJBQLBaseVisitor {
    
   
    private EJBQLTranslationContext context;
    private String lastTableAlias;

    public EJBQLFromTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
    }

    public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            if (lastTableAlias != null) {
                context.append(',');
            }

            lastTableAlias = appendTable(expression.getId());
        }
        return true;
    }

    public boolean visitInnerFetchJoin(EJBQLJoin join, int finishedChildIndex) {
        // TODO: andrus, 4/9/2007 - support for prefetching
        return visitInnerJoin(join, finishedChildIndex);
    }

    public boolean visitInnerJoin(EJBQLJoin join, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            appendJoin(join, "INNER JOIN");
        }
        return true;
    }

    public boolean visitOuterFetchJoin(EJBQLJoin join, int finishedChildIndex) {
        // TODO: andrus, 4/9/2007 - support for prefetching
        return visitOuterJoin(join, finishedChildIndex);
    }

    public boolean visitOuterJoin(EJBQLJoin join, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            appendJoin(join, "LEFT OUTER JOIN");
        }
        return true;
    }

    private void appendJoin(EJBQLJoin join, String semantics) {

        String id = join.getId();

        if (lastTableAlias == null) {
            throw new EJBQLException("No source table for join: " + id);
        }

        String sourceAlias = lastTableAlias;

        context.append(" ").append(semantics);
        String targetAlias = appendTable(id);
        context.append(" ON (");

        ObjRelationship incoming = context
                .getCompiledExpression()
                .getIncomingRelationship(id);
        if (incoming == null) {
            throw new EJBQLException("No join configured for id " + id);
        }

        // TODO: andrus, 4/8/2007 - support for flattened relationships
        DbRelationship incomingDB = (DbRelationship) incoming.getDbRelationships().get(0);

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
        this.lastTableAlias = targetAlias;
    }

    private String appendTable(String id) {
        ClassDescriptor descriptor = context.getCompiledExpression().getEntityDescriptor(
                id);

        String tableName = descriptor.getEntity().getDbEntity().getFullyQualifiedName();
        String alias = context.getAlias(id, tableName);
        context.append(' ').append(tableName).append(" AS ").append(alias);
        return alias;
    }
}

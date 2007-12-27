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

import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Handles appending joins to the content buffer at a marked position.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLJoinAppender {

    protected EJBQLTranslationContext context;

    public EJBQLJoinAppender(EJBQLTranslationContext context) {
        this.context = context;
    }

    public void appendInnerJoin(String marker, String lhsId, String rhsId) {
        appendJoin(marker, lhsId, rhsId, "INNER JOIN");
    }

    public void appendOuterJoin(String marker, String lhsId, String rhsId) {
        appendJoin(marker, lhsId, rhsId, "LEFT OUTER JOIN");
    }

    protected void appendJoin(String marker, String lhsId, String rhsId, String semantics) {

        ObjRelationship joinRelationship = context.getIncomingRelationship(rhsId);
        if (joinRelationship == null) {
            throw new EJBQLException("No join configured for id " + rhsId);
        }

        // TODO: andrus, 4/8/2007 - support for flattened relationships
        DbRelationship incomingDB = joinRelationship.getDbRelationships().get(0);

        String sourceAlias = context.getTableAlias(lhsId, incomingDB
                .getSourceEntity()
                .getName());

        if (marker != null) {
            context.switchToMarker(marker, false);
        }

        try {

            context.append(" ").append(semantics);
            String targetAlias = appendTable(rhsId);
            context.append(" ON (");

            Iterator<DbJoin> it = incomingDB.getJoins().iterator();
            if (it.hasNext()) {
                DbJoin dbJoin = it.next();
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
                DbJoin dbJoin = it.next();
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
        finally {
            if (marker != null) {
                context.switchToMainBuffer();
            }
        }
    }

    public String appendTable(String id) {
        ClassDescriptor descriptor = context.getEntityDescriptor(id);

        String tableName = descriptor.getEntity().getDbEntity().getFullyQualifiedName();

        if (context.isUsingAliases()) {
            String alias = context.getTableAlias(id, tableName);

            // not using "AS" to separate table name and alias name - OpenBase doesn't
            // support
            // "AS", and the rest of the databases do not care
            context.append(' ').append(tableName).append(' ').append(alias);
            return alias;
        }
        else {
            context.append(' ').append(tableName);
            return tableName;
        }
    }

    static String makeJoinTailMarker(String id) {
        return "FROM_TAIL" + id;
    }
}
